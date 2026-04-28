package com.zeroffa.tdbinpacking.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJsonParser {

    private final String input;
    private int index;

    SimpleJsonParser(String input) {
        this.input = input == null ? "" : input;
    }

    Object parse() {
        Object value = parseValue();
        skipWhitespace();
        if (index != input.length()) {
            throw error("unexpected trailing content");
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= input.length()) {
            throw error("unexpected end of input");
        }
        char ch = input.charAt(index);
        if (ch == '{') {
            return parseObject();
        }
        if (ch == '[') {
            return parseArray();
        }
        if (ch == '"') {
            return parseString();
        }
        if (ch == 't') {
            expect("true");
            return Boolean.TRUE;
        }
        if (ch == 'f') {
            expect("false");
            return Boolean.FALSE;
        }
        if (ch == 'n') {
            expect("null");
            return null;
        }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        consume('{');
        skipWhitespace();
        if (peek('}')) {
            consume('}');
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            consume(':');
            result.put(key, parseValue());
            skipWhitespace();
            if (peek('}')) {
                consume('}');
                return result;
            }
            consume(',');
        }
    }

    private List<Object> parseArray() {
        List<Object> result = new ArrayList<Object>();
        consume('[');
        skipWhitespace();
        if (peek(']')) {
            consume(']');
            return result;
        }
        while (true) {
            result.add(parseValue());
            skipWhitespace();
            if (peek(']')) {
                consume(']');
                return result;
            }
            consume(',');
        }
    }

    private String parseString() {
        consume('"');
        StringBuilder builder = new StringBuilder();
        while (index < input.length()) {
            char ch = input.charAt(index++);
            if (ch == '"') {
                return builder.toString();
            }
            if (ch != '\\') {
                builder.append(ch);
                continue;
            }
            if (index >= input.length()) {
                throw error("unterminated escape");
            }
            char escaped = input.charAt(index++);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    builder.append(escaped);
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    builder.append(parseUnicode());
                    break;
                default:
                    throw error("invalid escape: " + escaped);
            }
        }
        throw error("unterminated string");
    }

    private char parseUnicode() {
        if (index + 4 > input.length()) {
            throw error("invalid unicode escape");
        }
        String hex = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            throw error("invalid unicode escape: " + hex);
        }
    }

    private BigDecimal parseNumber() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        readDigits();
        if (peek('.')) {
            index++;
            readDigits();
        }
        if (peek('e') || peek('E')) {
            index++;
            if (peek('+') || peek('-')) {
                index++;
            }
            readDigits();
        }
        if (start == index) {
            throw error("expected value");
        }
        return new BigDecimal(input.substring(start, index));
    }

    private void readDigits() {
        int start = index;
        while (index < input.length() && Character.isDigit(input.charAt(index))) {
            index++;
        }
        if (start == index) {
            throw error("expected digit");
        }
    }

    private void expect(String value) {
        if (!input.startsWith(value, index)) {
            throw error("expected " + value);
        }
        index += value.length();
    }

    private void consume(char expected) {
        skipWhitespace();
        if (index >= input.length() || input.charAt(index) != expected) {
            throw error("expected '" + expected + "'");
        }
        index++;
    }

    private boolean peek(char expected) {
        return index < input.length() && input.charAt(index) == expected;
    }

    private void skipWhitespace() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at character " + index);
    }
}
