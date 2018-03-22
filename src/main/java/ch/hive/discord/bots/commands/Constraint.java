package ch.hive.discord.bots.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Constraint {
    Class[] value();
    boolean enforceAll() default true;
}
