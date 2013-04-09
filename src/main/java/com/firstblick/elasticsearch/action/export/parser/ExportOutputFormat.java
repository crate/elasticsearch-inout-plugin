package com.firstblick.elasticsearch.action.export.parser;

/**
 * Defines the format of the exported content.
 */
public class ExportOutputFormat {

    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_DELIMITED = "delimited";

    private String format = FORMAT_DELIMITED;
    private Character delimiter = '\u0001';
    private String nullSequence = "\\N";


    public ExportOutputFormat() {
    }

    public ExportOutputFormat(String format) {
        if (!isIn(format, FORMAT_JSON, FORMAT_DELIMITED)) {
            throw new IllegalArgumentException("output_format must be one of 'json' or 'delimited'");
        }
        this.format = format;
    }

    public ExportOutputFormat(String format, Character delimiter, String nullSequence) {
        this(format);
        this.delimiter = delimiter;
        this.nullSequence = nullSequence;
    }

    public void delimiter(Character delimiter) {
        this.delimiter = delimiter;
    }

    public void nullSequence(String nullSequence) {
        this.nullSequence = nullSequence;
    }

    public <T> boolean isIn(T t, T... ts) {
        for (T t2 : ts)
            if (t.equals(t2)) return true;
        return false;
    }
}
