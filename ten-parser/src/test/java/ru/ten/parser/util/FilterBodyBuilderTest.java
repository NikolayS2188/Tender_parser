package ru.ten.parser.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilterBodyBuilderTest {

    private static final String START = "01.01.2024";
    private static final String END = "09.10.2026 23:59:59";

    @Test
    void shouldBuildOneWordFilterBodyWithStatus() {
        String xml = FilterBodyBuilder.buildOneWord(
                "kaspersky", "В процессе проведения", 0, 20, START, END);

        assertThat(xml)
                .contains("<searchBarType>oneWord</searchBarType>")
                .contains("<mainInput>kaspersky</mainInput>")
                .contains("<purchStateNameTerm>В процессе проведения</purchStateNameTerm>")
                .contains("<pagenum>0</pagenum>")
                .contains("<pagesize>20</pagesize>")
                .contains("<startPublicDate>01.01.2024</startPublicDate>")
                .contains("<endPublicDate>09.10.2026 23:59:59</endPublicDate>")
                .contains("<sortOrder>desc</sortOrder>")
                .startsWith("<query>")
                .endsWith("</query>");
    }

    @Test
    void shouldBuildOneWordFilterBodyWithoutStatus() {
        String xml = FilterBodyBuilder.buildOneWord(
                "usb", null, 1, 10, START, END);

        assertThat(xml)
                .contains("<mainInput>usb</mainInput>")
                .doesNotContain("<purchStateNameTerm>")
                .contains("<pagenum>1</pagenum>")
                .contains("<pagesize>10</pagesize>");
    }

    @Test
    void shouldBuildAnyWordFilterBody() {
        String xml = FilterBodyBuilder.buildAnyWord(
                List.of("kaspersky", "usb"), "Подача заявок", 0, 20, START, END);

        assertThat(xml)
                .contains("<searchBarType>anyWord</searchBarType>")
                .contains("<mainInput>kaspersky usb</mainInput>")
                .contains("<purchStateNameTerm>Подача заявок</purchStateNameTerm>");
    }

    @Test
    void shouldEscapeXmlSpecialCharsInKeyword() {
        String xml = FilterBodyBuilder.buildOneWord(
                "a & b < c", null, 0, 20, START, END);

        assertThat(xml)
                .contains("<mainInput>a &amp; b &lt; c</mainInput>");
    }

    @Test
    void shouldEscapeXmlSpecialCharsInStatus() {
        String xml = FilterBodyBuilder.buildOneWord(
                "kaspersky", "A & B < C", 0, 20, START, END);

        assertThat(xml)
                .contains("<purchStateNameTerm>A &amp; B &lt; C</purchStateNameTerm>");
    }
}
