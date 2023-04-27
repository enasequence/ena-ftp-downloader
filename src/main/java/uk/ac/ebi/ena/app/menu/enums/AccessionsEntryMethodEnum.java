/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Getter
@AllArgsConstructor
public enum AccessionsEntryMethodEnum {
    DOWNLOAD_FROM_FILE(1, "provide a file path"),
    DOWNLOAD_FROM_LIST(2, "provide an accession or a list of accessions separated by commas"),
    DOWNLOAD_FROM_QUERY(3, "provide a search query");

    private final int value;
    private final String message;
    private final static Map<Integer, AccessionsEntryMethodEnum> map = new HashMap<>();

    static {
        Arrays.stream(AccessionsEntryMethodEnum.values()).forEach(k -> map.put(k.value, k));
    }

    public static AccessionsEntryMethodEnum valueOf(Integer i) {
        return map.get(i);
    }

}
