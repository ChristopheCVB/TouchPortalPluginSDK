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

package com.christophecvb.touchportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Connector Value Annotation
 * <p>
 * Target is a Parameter
 * </p>
 * <p>
 * <b>When annotating a method with {@link Connector} annotation, you have to annotate one (1) Integer method parameter with ConnectorValue annotation</b>
 * <i>If it is not the case, the SDK will call the TouchPortalPluginListener.onReceive(JsonObject jsonMessage) instead.</i>
 * </p>
 *
 * @see <a href="https://www.touch-portal.com/api/index.php?section=connectors">TP Documentation: Connectors</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ConnectorValue {
}
