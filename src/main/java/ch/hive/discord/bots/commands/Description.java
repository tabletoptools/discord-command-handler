/*
 * Copyright Carlo Field (cfi@bluesky-it.ch)
 */
package ch.hive.discord.bots.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author kileraptor1
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    String value();
}
