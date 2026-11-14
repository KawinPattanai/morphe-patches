/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4764
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.shared.patches;

import com.google.android.libraries.youtube.media.interfaces.HttpHeader;

import java.lang.reflect.Field;

/**
 * Reads the name and the value of a header of the media requests of the app.
 *
 * The patch of upstream adds {@link MediaHttpHeader} to the header class of the app while patching,
 * which the module cannot do, so the two strings are read from the fields the class carries instead.
 */
public final class HttpHeaderAccess {

    private HttpHeaderAccess() {
    }

    private static Field[] stringFields;

    /** Whether the fields were handed over already, so the class is not looked at again. */
    public static boolean isResolved() {
        return stringFields != null;
    }

    /**
     * The fields the module resolved from the bytecode of the app, which name and value the header
     * of a media request are. Set once while patching, in the process the patches run in.
     */
    public static void setFields(Field name, Field value) {
        name.setAccessible(true);
        value.setAccessible(true);
        stringFields = new Field[]{name, value};
    }

    public static String name(HttpHeader header) {
        return string(header, 0);
    }

    public static String value(HttpHeader header) {
        return string(header, 1);
    }

    private static String string(HttpHeader header, int index) {
        if (header instanceof MediaHttpHeader) {
            MediaHttpHeader patched = (MediaHttpHeader) header;
            return index == 0 ? patched.patch_getName() : patched.patch_getValue();
        }

        Field[] fields = stringFields(header.getClass());
        if (fields.length <= index) {
            return null;
        }
        try {
            String value = (String) fields[index].get(header);
            if (index == 0 && !looksLikeHeaderName(value)) {
                // The fields of the class are read in the order it declares them, which another build
                // may swap: a value that cannot be a header name keeps the header instead of dropping
                // it, so a wrong read costs a header that is sent twice rather than a request that fails.
                return null;
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean looksLikeHeaderName(String value) {
        if (value == null || value.isEmpty() || value.length() > 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    /** The first two string fields of the header class, which are its name and value. */
    private static synchronized Field[] stringFields(Class<?> type) {
        if (stringFields == null) {
            Field name = null;
            Field value = null;
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    if (name == null) {
                        name = field;
                    } else {
                        value = field;
                        break;
                    }
                }
            }
            stringFields = value == null ? new Field[0] : new Field[]{name, value};
        }
        return stringFields;
    }
}
