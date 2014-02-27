package com.nosoop.json.vdf;

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 nosoop
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Provides static methods to convert a file from the Valve Data Format (VDF) to
 * an equivalent JSON representation.
 *
 * Support is basic and disgusting. It also makes certain assumptions of the
 * file (e.g., it assumes every odd-numbered string is a key, while the string
 * to its right is its corresponding value.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class VDF {

    /**
     * Opening brace character. Used to signal the start of a nested KeyValue
     * set.
     */
    public static final char L_BRACE = '{';
    /**
     * Closing brace character. Used to signal the end of a nested KeyValue set.
     */
    public static final char R_BRACE = '}';
    /**
     * Forward slash character. Used in C++ styled comments.
     */
    public static final char SLASH = '/';
    /**
     * Backward slash character. Used to escape strings.
     */
    public static final char BACK_SLASH = '\\';
    /**
     * Quote character. Used to signal the start of a String (key or value).
     */
    public static final char QUOTE = '"';
    /**
     * Newline character. Essentially whitespace, but we need it when we're
     * skipping C++ styled comments.
     */
    public static final char NEWLINE = '\n';

    /**
     * Utility method to parse a VDF value.
     *
     * @param x The JSONTokener to use.
     * @param delimiter The character that signals the end of the
     * @return
     * @throws JSONException
     */
    private static String getVDFValue(JSONTokener x, final char delimiter) throws JSONException {
        StringBuilder sb = new StringBuilder();

        while (x.more()) {
            char c = x.next();
            switch (c) {
                case BACK_SLASH:
                    // Unescape character.
                    sb.append(x.next());
                    break;
                default:
                    // Return the string if the tokener hit the delimiter.
                    // (If it was escaped, it was handled in the previous case.
                    if (c == delimiter) {
                        return sb.toString();
                    } // Otherwise, append it to the string.
                    else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }

    /**
     * Attempts to convert what is assumed to be a JSONTokener containing a
     * String with VDF text into the JSON format.
     *
     * @param string Input data, assumed to be in the Valve Data Format.
     * @return A JSON representation of the assumed-VDF data.
     * @throws JSONException Parse exception?
     */
    public static JSONObject toJSONObject(JSONTokener x) throws JSONException {
        JSONObject jo = new JSONObject();

        while (x.more()) {
            char c = x.nextClean();

            switch (c) {
                case QUOTE:
                    // Case that it is a String key and we should expect its value next.
                    String key = x.nextString(QUOTE);

                    char ctl = x.nextClean();
                    switch (ctl) {
                        case SLASH:
                            if (x.next() == SLASH) {
                                // Comment -- ignore the rest of the line.
                                x.skipTo(NEWLINE);
                                ctl = x.nextClean();
                            }
                    }

                    // Case that the next thing is another String value; add.
                    if (ctl == QUOTE) {
                        String value = getVDFValue(x, QUOTE);
                        jo.put(key, value);
                    } // Or a nested KeyValue pair. Parse then add.
                    else if (ctl == L_BRACE) {
                        jo.put(key, toJSONObject(x));
                    }

                    // TODO Add support for bracketed tokens?

                    break;
                case R_BRACE:
                    // Case that we are done parsing this KeyValue collection.
                    // Return it (back to the calling toJSONObject() method).
                    return jo;
                case '\0':
                    // Disregard null character.
                    break;
                case SLASH:
                    if (x.next() == SLASH) {
                        // It's a comment. Skip to the next line.
                        x.skipTo(NEWLINE);
                        break;
                    }
                default:
                    String fmtError = "Unexpected character \'%s\'";
                    throw x.syntaxError(String.format(fmtError, c));
            }
        }
        return jo;
    }

    /**
     * Attempts to convert what is assumed to be a String containing VDF text
     * into the JSON format.
     *
     * @param string Input data, assumed to be in the Valve Data Format.
     * @return A JSON representation of the assumed-VDF data.
     * @throws JSONException Parse exception?
     */
    public static JSONObject toJSONObject(String string) throws JSONException {
        return toJSONObject(new JSONTokener(string));
    }
}
