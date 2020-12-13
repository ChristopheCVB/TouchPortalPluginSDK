/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.ChristopheCVB.TouchPortal.Helpers;

/**
 * Touch Portal Plugin Message Helper
 */
public class SentMessageHelper {
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_STATE_UPDATE = "stateUpdate";
    public static final String TYPE_PAIR = "pair";
    public static final String TYPE_CHOICE_UPDATE = "choiceUpdate";
    public static final String TYPE_CREATE_STATE = "createState";
    public static final String TYPE_REMOVE_STATE = "removeState";
    public static final String TYPE_ACTION_DATA_UPDATE = "updateActionData";
    public static final String TYPE_SETTING_UPDATE = "settingUpdate";
    public static final String INSTANCE_ID = "instanceId";
    public static final String ID = GenericHelper.ID;
    public static final String VALUE = GenericHelper.VALUE;
    public static final String DATA = "data";
    public static final String NAME = "name";
    public static final String DESCRIPTION = StateHelper.DESC;
    public static final String DEFAULT_VALUE = "defaultValue";
}