/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.ucte.network;

import org.junit.Test;

import static eu.itesla_project.ucte.network.UcteNodeTypeCode.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class UcteNodeTypeCodeTest {

    @Test
    public void test() {
        assertEquals(4, values().length);
        assertArrayEquals(new UcteNodeTypeCode[] {PQ, QT, PU, UT}, UcteNodeTypeCode.values());
    }
}
