package com.nosoop.json;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
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
     * Attempts to convert what is assumed to be a JSONTokener containing a
     * String with VDF text into the JSON format.
     *
     * @param x A JsonTokener instantiated with VDF data.
     * @param convertArrays Whether or not to convert VDF-formatted arrays into
     * JSONArrays.
     * @return A JSON representation of the assumed-VDF data.
     * @throws JSONException Parse exception?
     */
    public static JSONObject toJSONObject(JSONTokener x, boolean convertArrays)
            throws JSONException {
        JSONObject jo = new JSONObject();

        while (x.more()) {
            char c = x.nextClean();

            switch (c) {
                case QUOTE:
                    // Case that it is a String key, expect its value next.
                    String key = x.nextString(QUOTE);

                    char ctl = x.nextClean();
                    if (ctl == SLASH) {
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
                        jo.put(key, toJSONObject(x, convertArrays));
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

        if (convertArrays) {
            return convertVDFArrays(jo);
        }

        return jo;
    }

    /**
     * Attempts to convert what is assumed to be a String containing VDF text
     * into the JSON format.
     *
     * @param string Input data, assumed to be in the Valve Data Format.
     * @param convertArrays Whether or not to convert VDF-formatted arrays into
     * JSONArrays.
     * @return A JSON representation of the assumed-VDF data.
     * @throws JSONException Parse exception?
     */
    public static JSONObject toJSONObject(String string, boolean convertArrays)
            throws JSONException {
        return toJSONObject(new JSONTokener(string), convertArrays);
    }

    /**
     * Utility method to parse a VDF value.
     *
     * @param x The JSONTokener to use.
     * @param delimiter The character that signals the end of the value.
     * @return String extracted from the JSONTokener's current position up to the delimiter, with certain characters escaped.
     * @throws JSONException
     */
    private static String getVDFValue(JSONTokener x, final char delimiter)
            throws JSONException {
        StringBuilder sb = new StringBuilder();

        while (x.more()) {
            char c = x.next();
            switch (c) {
                case BACK_SLASH:
                    // Unescape character.
                    // -- Allowed Escape sequences are \n, \t, \\, and \".
                    char u = x.next();
                    switch (u) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '\\':
                        case '\"':
                            sb.append(u);
                            break;
                        default:
                            String fmtError = 
                                    "Unexpected escape sequence \"\\%s\"";
                            throw x.syntaxError(String.format(fmtError, u));
                    }
                    break;
                default:
                    // Return the string if the tokener hit the delimiter.
                    // (If it was escaped, it was handled in the previous case.)
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
     * Recursively searches for JSONObjects, checking if they should be
     * formatted as arrays, then converted.
     *
     * @param object An input JSONObject converted from VDF.
     * @return JSONObject containing the input JSONObject with objects changed
     * to arrays where applicable.
     * @throws JSONException
     */
    private static JSONObject convertVDFArrays(JSONObject object) throws JSONException {
        JSONObject resp = new JSONObject();

        if (object.keySet().isEmpty()) {
            return resp;
        }

        for (String name : (Set<String>) object.keySet()) {
            JSONObject thing = object.optJSONObject(name);

            if (thing != null) {
                // Note:  Empty JSONObjects are also treated as arrays.
                if (containsVDFArray(thing)) {
                    List<String> sortingKeys = new ArrayList(thing.keySet());

                    Collections.sort(sortingKeys, new Comparator<String>() {
                        // Integers-as-strings comparator.
                        @Override
                        public int compare(String t, String t1) {
                            int i = Integer.parseInt(t), i1 = Integer.parseInt(t1);
                            return i - i1;
                        }
                    });

                    JSONArray sortedKeys = new JSONArray(sortingKeys);

                    if (sortedKeys.length() > 0) {
                        JSONArray sortedObjects = thing.toJSONArray(sortedKeys);

                        for (int i = 0; i < sortedObjects.length(); i++) {
                            JSONObject arrayObject = sortedObjects.getJSONObject(i);

                            /**
                             * See if any values are also JSONObjects that
                             * should be arrays.
                             */
                            sortedObjects.put(i, convertVDFArrays(arrayObject));
                        }

                        /**
                         * If this JSONObject represents a non-empty array in
                         * VDF format, convert it to a JSONArray.
                         */
                        resp.put(name, sortedObjects);
                    } else {
                        /**
                         * If this JSONObject represents an empty array, give it
                         * an empty JSONArray.
                         */
                        resp.put(name, new JSONArray());
                    }
                } else {
                    /**
                     * If this JSONObject is not a VDF array, see if its values
                     * are before adding.
                     */
                    resp.put(name, convertVDFArrays(thing));
                }
            } else {
                /**
                 * It's a plain data value. Add it in.
                 */
                resp.put(name, object.get(name));
            }
        }

        /**
         * Return the converted JSONObject.
         */
        return resp;
    }

    /**
     * Checks that a JSONObject converted from a VDF file is an array. If so,
     * the only keys in the JSONObject are a continues set of integers
     * represented by Strings starting from "0". Note that empty JSONObjects are
     * also treated as arrays.
     *
     * @param object The JSONObject to check for a VDF-formatted array.
     * @return Whether or not the JSONObject is a VDF-formatted array.
     */
    private static boolean containsVDFArray(JSONObject object) {
        int indices = object.length();
        int[] index = new int[indices];

        for (int i = 0; i < indices; i++) {
            index[i] = -1;
        }

        /**
         * Fail if we encounter a non-integer, if a value isn't a JSONObject,
         * or if the key is a number that is larger than the size of the array
         * (meaning we're missing a value).
         */
        for (String name : (Set<String>) object.keySet()) {
            if (object.optJSONObject(name) == null) {
                return false;
            }

            try {
                int i = Integer.parseInt(name);
                
                if (i >= indices) {
                    return false;
                }
                
                index[i] = i;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Fail if we are missing any values (e.g., 0, 1, 2, 3, 4, 5, 7, 8, 9).
        for (int i = 0; i < indices; i++) {
            if (index[i] != i) {
                return false;
            }
        }

        return true;
    }
}
