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
     * Gets the next String value, starting from the current character.
     *
     * @param x The JSONTokener to act on.
     * @return String value.
     * @throws JSONException
     */
    private static String getStringValue(JSONTokener x) throws JSONException {
        StringBuilder sb = new StringBuilder();

        char c = x.next();

        if (c == '\"') {
            boolean done = false;
            while (!done) {
                c = x.next();
                switch (c) {
                    case '\\':
                        // Unescape the character -- it'll be reescaped in JSON.
                        sb.append(x.next());
                        break;
                    case '\"':
                        // End of quited value.
                        done = true;
                        break;
                    default:
                        sb.append(c);
                }
            }
        }

        return sb.toString();
    }

    public static JSONObject toJSONObject(JSONTokener x) throws JSONException {
        JSONObject jo = new JSONObject();

        while (x.more()) {
            char c = x.nextClean();

            switch (c) {
                case '\"':
                    // Case that it is a String value.
                    x.back();
                    String key = getStringValue(x);

                    char ctl = x.nextClean();

                    // Case that the next thing is another String value, then put.
                    if (ctl == '\"') {
                        x.back();
                        String value = getStringValue(x);
                        jo.put(key, value);
                    } // Or a nested KeyValue pair.
                    else if (ctl == '{') {
                        jo.put(key, toJSONObject(x));
                    }

                    break;
                case '}':
                    // Case that we are done parsing this KeyValue collection.
                    return jo;
            }
        }
        return jo;
    }

    /**
     * Attempts to convert what is assumed to be a VDF file into the JSON
     * format.
     *
     * @param string Input data, assumed to be in the Valve Data Format.
     * @return A JSON representation of the assumed-VDF data.
     * @throws JSONException Parse exception?
     */
    public static JSONObject toJSONObject(String string) throws JSONException {
        return toJSONObject(new JSONTokener(string));
    }
}
