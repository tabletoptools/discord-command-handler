/*
 * $Id: ParamDescriptor 3988 2017-06-21 13:47:09Z cfi $
 * Created on Aug 10, 2017, 5:20:50 PM
 * 
 * Copyright (c) 2017 by bluesky IT-Solutions AG,
 * Kaspar-Pfeiffer-Strasse 4, 4142 Muenchenstein, Switzerland.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of bluesky IT-Solutions AG ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with bluesky IT-Solutions AG.
 */
package ch.hive.discord.bots.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author cfi
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    String value();
}
