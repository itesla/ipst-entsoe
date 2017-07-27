/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.cim1.converter;

import cim1.model.CIMModel;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class CIM1DefaultNamingStrategyFactory implements CIM1NamingStrategyFactory {

    @Override
    public CIM1NamingStrategy create(CIMModel model) {
        return new CIM1DefaultNamingStrategy();
    }

}
