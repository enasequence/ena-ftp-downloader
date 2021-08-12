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

package uk.ac.ebi.ena.app.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MenuUtilsTest {

    @Test
    public void testAccFromFile() {
        String accFilePath = "src/test/resources/accFileHeader";
        List<String> accessionIds = MenuUtils.accsFromFile(accFilePath);
        List<String> expectedAccIds = new ArrayList<>();
        expectedAccIds.add("DRR274673");
        expectedAccIds.add("DRR274674");
        expectedAccIds.add("DRR274675");
        Assertions.assertTrue(checkIfListIsSame(expectedAccIds, accessionIds));
    }

    @Test
    public void testAccFromFileContainsQuotes() {
        String accFilePath = "src/test/resources/accFileQuotes";
        List<String> accessionIds = MenuUtils.accsFromFile(accFilePath);
        List<String> expectedAccIds = new ArrayList<>();
        expectedAccIds.add("DRX264257");
        expectedAccIds.add("DRX264258");
        expectedAccIds.add("DRX264259");
        Assertions.assertTrue(checkIfListIsSame(expectedAccIds, accessionIds));
    }

    @Test
    public void testAccFromFileOnlyHeader() {
        String accFilePath = "src/test/resources/accFileOnlyHeader";
        List<String> accessionIds = MenuUtils.accsFromFile(accFilePath);
        Assertions.assertEquals(0, accessionIds.size());
    }

    @Test
    public void testAccFromFile_WhenEmptyAccFile() {
        String accFilePath = "src/test/resources/accFile_empty";
        List<String> accessionIds = MenuUtils.accsFromFile(accFilePath);
        Assertions.assertEquals(0, accessionIds.size());

    }

    private boolean checkIfListIsSame(List<String> expected, List<String> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int i = 0; i < actual.size(); i++) {
            if (!(expected.get(i).equals(actual.get(i)))) {
                return false;
            }
        }
        return true;

    }
}
