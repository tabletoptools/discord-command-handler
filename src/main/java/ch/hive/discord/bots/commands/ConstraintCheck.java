/*
 * $Id: HttpFilter 3988 2017-06-21 13:47:09Z cfi $
 * Created on 11.11.17 07:31
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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class ConstraintCheck {

    public abstract boolean check(MessageReceivedEvent event);

    public String errorMessage() {
        return "";
    }

}
