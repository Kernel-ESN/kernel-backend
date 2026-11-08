package com.kernelconsulting.plateforme.listing.repository;

import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.listing.api.dto.ListingFacetsResponse;
import com.kernelconsulting.plateforme.listing.api.dto.PublicOrganizationResponse;
import com.kernelconsulting.plateforme.listing.domain.ListingSort;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.services.ListingSearch;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Recherche plein texte en français sur PostgreSQL. Les valeurs passent toujours en paramètres. */
@Repository
@RequiredArgsConstructor
public class ListingSearchRepository {

    private static final long DAY_SECONDS = 86_400L;

    private final JdbcClient jdbc;

    /** Identifiants d'une page de résultats et nombre total. */
    public record Result(List<UUID> ids, long total) {
    }

    /** Filtres qu'un compteur ignore : chaque filtre compte ce que donnerait le choix d'une autre valeur. */
    private enum Dimension { TYPE, SECTOR, REGION, EDUCATION, ORGANIZATION_TYPE, DEADLINE, PUBLISHED }

    private record Clause(String sql, Map<String, Object> params) {
    }

    public Result search(ListingSearch search, Instant now) {
        Clause clause = clause(search, now, EnumSet.noneOf(Dimension.class));
        Map<String, Object> params = new HashMap<>(clause.params());
        long total = jdbc.sql("SELECT count(*)" + clause.sql()).params(params).query(Long.class).single();
        params.put("limit", search.limit());
        params.put("offset", search.offset());
        List<UUID> ids = jdbc.sql("SELECT id" + clause.sql() + orderBy(search.sort(), search.text() != null)
                        + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query(UUID.class)
                .list();
        return new Result(ids, total);
    }

    public ListingFacetsResponse facets(ListingSearch search, Instant now) {
        return new ListingFacetsResponse(
                countBy("type", search, now, Dimension.TYPE),
                countBy("sector_id", search, now, Dimension.SECTOR),
                countBy("region_id", search, now, Dimension.REGION),
                countBy("education_level", search, now, Dimension.EDUCATION),
                countByOrganizationType(search, now),
                windows(search, now, Dimension.DEADLINE, "deadline <= :limit_%d", "limit_%d", 1, List.of(7, 30)),
                windows(search, now, Dimension.PUBLISHED, "published_at >= :limit_%d", "limit_%d", -1, List.of(1, 7, 30)));
    }

    /** Organisations ayant au moins une annonce ouverte, les plus actives d'abord. */
    public List<PublicOrganizationResponse> organizations(Instant now) {
        return jdbc.sql(ORGANIZATIONS + " GROUP BY o.id HAVING count(l.id) FILTER (WHERE l.deadline >= :now) > 0"
                        + " ORDER BY open_listings DESC, o.name, o.id")
                .param("now", Timestamp.from(now))
                .query(ListingSearchRepository::organization)
                .list();
    }

    /** Une organisation dont les annonces ont toutes expiré reste consultable : sa fiche y renvoie. */
    public Optional<PublicOrganizationResponse> organization(String slug, Instant now) {
        return jdbc.sql(ORGANIZATIONS + " AND o.slug = :slug GROUP BY o.id")
                .param("now", Timestamp.from(now))
                .param("slug", slug)
                .query(ListingSearchRepository::organization)
                .optional();
    }

    private static final String ORGANIZATIONS =
            "SELECT o.id, o.slug, o.name, o.type, o.verification,"
                    + " count(l.id) FILTER (WHERE l.deadline >= :now) AS open_listings"
                    + " FROM organization o JOIN listing l ON l.organization_id = o.id"
                    + " WHERE l.status = 'PUBLIEE'";

    private static PublicOrganizationResponse organization(ResultSet row, int index) throws SQLException {
        return new PublicOrganizationResponse(
                row.getObject("id", UUID.class),
                row.getString("slug"),
                row.getString("name"),
                row.getString("type"),
                "VERIFIEE".equals(row.getString("verification")),
                row.getLong("open_listings"));
    }

    private Map<String, Long> countBy(String column, ListingSearch search, Instant now, Dimension skip) {
        Clause clause = clause(search, now, EnumSet.of(skip));
        return collect(jdbc.sql("SELECT " + column + " AS key, count(*) AS total" + clause.sql()
                        + " AND " + column + " IS NOT NULL GROUP BY " + column)
                .params(clause.params()));
    }

    private Map<String, Long> countByOrganizationType(ListingSearch search, Instant now) {
        Clause clause = clause(search, now, EnumSet.of(Dimension.ORGANIZATION_TYPE));
        return collect(jdbc.sql("SELECT o.type AS key, count(*) AS total FROM organization o"
                        + " JOIN (SELECT organization_id" + clause.sql() + ") f ON f.organization_id = o.id GROUP BY o.type")
                .params(clause.params()));
    }

    private static Map<String, Long> collect(JdbcClient.StatementSpec statement) {
        Map<String, Long> counts = new LinkedHashMap<>();
        statement.query((ResultSet row) -> {
            counts.put(row.getString("key"), row.getLong("total"));
        });
        return counts;
    }

    /** Nombre d'annonces pour chaque durée proposée, zéro compris. */
    private Map<String, Long> windows(
            ListingSearch search, Instant now, Dimension skip, String condition, String param, int sign, List<Integer> days) {
        Clause clause = clause(search, now, EnumSet.of(skip));
        Map<String, Object> params = new HashMap<>(clause.params());
        List<String> columns = new ArrayList<>();
        for (int day : days) {
            params.put(param.formatted(day), Timestamp.from(now.plusSeconds(sign * day * DAY_SECONDS)));
            columns.add("count(*) FILTER (WHERE " + condition.formatted(day) + ")");
        }
        List<Long> totals = jdbc.sql("SELECT " + String.join(", ", columns) + clause.sql())
                .params(params)
                .query((row, index) -> {
                    List<Long> values = new ArrayList<>();
                    for (int column = 1; column <= days.size(); column++) {
                        values.add(row.getLong(column));
                    }
                    return values;
                })
                .single();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int position = 0; position < days.size(); position++) {
            counts.put(String.valueOf(days.get(position)), totals.get(position));
        }
        return counts;
    }

    private static Clause clause(ListingSearch search, Instant now, Set<Dimension> skip) {
        List<String> where = new ArrayList<>(List.of("status = 'PUBLIEE'", "deadline >= :now"));
        Map<String, Object> params = new HashMap<>();
        params.put("now", Timestamp.from(now));
        if (search.universe() != null) {
            where.add("universe = :universe");
            params.put("universe", search.universe().name());
        }
        if (!skip.contains(Dimension.TYPE) && !search.types().isEmpty()) {
            where.add("type IN (:types)");
            params.put("types", search.types().stream().map(ListingType::name).toList());
        }
        if (!skip.contains(Dimension.SECTOR) && !search.sectorIds().isEmpty()) {
            where.add("sector_id IN (:sectors)");
            params.put("sectors", search.sectorIds());
        }
        if (!skip.contains(Dimension.REGION) && !search.regionIds().isEmpty()) {
            where.add("region_id IN (:regions)");
            params.put("regions", search.regionIds());
        }
        if (!skip.contains(Dimension.EDUCATION) && !search.educationLevels().isEmpty()) {
            where.add("education_level IN (:educations)");
            params.put("educations", search.educationLevels());
        }
        if (!skip.contains(Dimension.ORGANIZATION_TYPE) && !search.organizationTypes().isEmpty()) {
            where.add("organization_id IN (SELECT id FROM organization WHERE type IN (:organizationTypes))");
            params.put("organizationTypes", search.organizationTypes().stream().map(OrganizationType::name).toList());
        }
        if (search.organizationId() != null) {
            where.add("organization_id = :organizationId");
            params.put("organizationId", search.organizationId());
        }
        if (!skip.contains(Dimension.DEADLINE) && search.deadlineWithinDays() != null) {
            where.add("deadline <= :latest");
            params.put("latest", Timestamp.from(now.plusSeconds(search.deadlineWithinDays() * DAY_SECONDS)));
        }
        if (!skip.contains(Dimension.PUBLISHED) && search.publishedWithinDays() != null) {
            where.add("published_at >= :earliest");
            params.put("earliest", Timestamp.from(now.minusSeconds(search.publishedWithinDays() * DAY_SECONDS)));
        }
        if (search.text() != null) {
            where.add("search @@ plainto_tsquery('french', f_unaccent(:text))");
            params.put("text", search.text());
        }
        return new Clause(" FROM listing WHERE " + String.join(" AND ", where), params);
    }

    private static String orderBy(ListingSort sort, boolean hasText) {
        if (sort == ListingSort.expire) {
            return " ORDER BY deadline ASC, id";
        }
        if (sort == ListingSort.pertinence && hasText) {
            return " ORDER BY ts_rank(search, plainto_tsquery('french', f_unaccent(:text))) DESC, published_at DESC, id";
        }
        return " ORDER BY published_at DESC, id";
    }
}
