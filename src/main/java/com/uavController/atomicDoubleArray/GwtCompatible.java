/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.uavController.atomicDoubleArray;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The presence of this annotation on a type indicates that the type may be used with the <a
 * href="http://code.google.com/webtoolkit/">Google Web Toolkit</a> (GWT). When applied to a method,
 * the return type of the method is GWT compatible. It's useful to indicate that an instance created
 * by factory methods has a GWT serializable type. In the following example,
 *
 * <pre>
 * {@literal @}GwtCompatible
 * class Lists {
 *   ...
 *   {@literal @}GwtCompatible(serializable = true)
 *   {@literal static <E> List<E>} newArrayList(E... elements) {
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>The return value of {@code Lists.newArrayList(E[])} has GWT serializable type. It is also
 * useful in specifying contracts of interface methods. In the following example,
 *
 * <pre>
 * {@literal @}GwtCompatible
 * interface ListFactory {
 *   ...
 *   {@literal @}GwtCompatible(serializable = true)
 *   {@literal <E> List<E>} newArrayList(E... elements);
 * }
 * </pre>
 *
 * <p>The {@code newArrayList(E[])} method of all implementations of {@code ListFactory} is expected
 * to return a value with a GWT serializable type.
 *
 * <p>Note that a {@code GwtCompatible} type may have some {@link GwtIncompatible} methods.
 *
 *
 * @author Charles Fry
 * @author Hayward Chan
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@GwtCompatible
public @interface GwtCompatible {

}
