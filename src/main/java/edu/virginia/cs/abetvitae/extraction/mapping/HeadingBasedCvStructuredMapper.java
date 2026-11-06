package edu.virginia.cs.abetvitae.extraction.mapping;

import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps common CV headings without executing document instructions or using network/tool access.
 */
@Component
public final class HeadingBasedCvStructuredMapper
        implements CvStructuredMapper {

    private static final int MAXIMUM_INPUT_CHARACTERS = 2_000_000;
    private static final String BULLET_PREFIX =
            "^(?:[\\p{Pd}*•▪◦]+|\\d+[.)])\\s*";
    private static final Map<String, ReviewSectionType> HEADINGS = headings();
    private static final List<ReviewSectionType> SECTION_ORDER = List.of(
            ReviewSectionType.PROFILE,
            ReviewSectionType.EDUCATION,
            ReviewSectionType.EXPERIENCE,
            ReviewSectionType.CREDENTIAL,
            ReviewSectionType.PROFESSIONAL_DEVELOPMENT,
            ReviewSectionType.SERVICE,
            ReviewSectionType.PUBLICATION_PRESENTATION
    );

    @Override
    public StructuredCvDraft map(ExtractedCvText extractedText) {
        if (extractedText == null
                || extractedText.text().length() > MAXIMUM_INPUT_CHARACTERS) {
            throw new CvMappingException(
                    "The extracted CV text exceeds the mapping limit"
            );
        }

        Map<ReviewSectionType, List<String>> entries =
                new EnumMap<>(ReviewSectionType.class);
        SECTION_ORDER.forEach(section -> entries.put(section, new ArrayList<>()));
        ReviewSectionType currentSection = null;
        for (String rawLine : extractedText.text().lines().toList()) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            Heading heading = heading(line);
            if (heading != null) {
                currentSection = heading.sectionType();
                if (!heading.inlineValue().isEmpty()) {
                    entries.get(currentSection).add(cleanEntry(
                            heading.inlineValue()
                    ));
                }
            } else if (currentSection != null) {
                String entry = cleanEntry(line);
                if (!entry.isEmpty()) {
                    entries.get(currentSection).add(entry);
                }
            }
        }

        List<MappedReviewSection> sections = new ArrayList<>();
        for (int order = 0; order < SECTION_ORDER.size(); order++) {
            ReviewSectionType sectionType = SECTION_ORDER.get(order);
            List<String> sectionEntries = List.copyOf(entries.get(sectionType));
            String sourceText = sectionEntries.isEmpty()
                    ? null
                    : String.join("\n", sectionEntries);
            sections.add(new MappedReviewSection(
                    sectionType,
                    sourceText,
                    sectionEntries,
                    order
            ));
        }
        return new StructuredCvDraft(
                sections,
                new MappingProvenance(
                        "heading-based",
                        "1",
                        null,
                        null
                )
        );
    }

    private static Heading heading(String line) {
        int separator = line.indexOf(':');
        String candidate = separator < 0 ? line : line.substring(0, separator);
        ReviewSectionType sectionType = HEADINGS.get(normalize(candidate));
        if (sectionType == null) {
            return null;
        }
        String inlineValue = separator < 0
                ? ""
                : line.substring(separator + 1).strip();
        return new Heading(sectionType, inlineValue);
    }

    private static String cleanEntry(String value) {
        return value.replaceFirst(BULLET_PREFIX, "").strip();
    }

    private static String normalize(String value) {
        return value.strip()
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .strip()
                .replaceAll("\\s+", " ");
    }

    private static Map<String, ReviewSectionType> headings() {
        Map<String, ReviewSectionType> headings = new LinkedHashMap<>();
        add(headings, ReviewSectionType.PROFILE,
                "Name", "Profile", "Faculty Profile");
        add(headings, ReviewSectionType.EDUCATION,
                "Education", "Academic Background");
        add(headings, ReviewSectionType.EXPERIENCE,
                "Academic and Professional Experience", "Experience",
                "Professional Experience", "Employment");
        add(headings, ReviewSectionType.CREDENTIAL,
                "Professional Credentials Certifications or Licensing",
                "Credentials", "Certifications", "Licenses");
        add(headings, ReviewSectionType.PROFESSIONAL_DEVELOPMENT,
                "Professional Development Activities",
                "Professional Development");
        add(headings, ReviewSectionType.SERVICE,
                "Service", "Professional Service", "Academic Service");
        add(headings, ReviewSectionType.PUBLICATION_PRESENTATION,
                "Contributions to the Discipline", "Publications",
                "Presentations", "Publications and Presentations");
        return Map.copyOf(headings);
    }

    private static void add(
            Map<String, ReviewSectionType> headings,
            ReviewSectionType sectionType,
            String... values
    ) {
        for (String value : values) {
            headings.put(normalize(value), sectionType);
        }
    }

    private record Heading(
            ReviewSectionType sectionType,
            String inlineValue) {
    }
}
