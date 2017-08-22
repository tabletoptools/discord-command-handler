/*
 * Copyright Carlo Field (cfi@bluesky-it.ch)
 */
package ch.hive.discord.bots.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 *
 * @author kileraptor1
 */
public class CommandBase {

    private static CommandBase instance;
    private final Collection<Method> methods;
    private String prefix = "";

    private CommandBase() {
        methods = new ArrayList<>();
    }

    public static CommandBase instance() {
        if (instance == null) {
            instance = new CommandBase();
        }
        return instance;
    }

    public CommandBase registerCommandClass(Class<?> klass) {
        while (klass != Object.class) {
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            allMethods.stream().filter((method) -> (method.isAnnotationPresent(Command.class))).forEachOrdered((method) -> {
                this.methods.add(method);
            });
            klass = klass.getSuperclass();
        }
        return this;
    }

    public CommandBase setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public void process(MessageReceivedEvent event) {
        if (!event.getMessage().getRawContent().startsWith(this.prefix)) {
            return;
        }

        String[] splitMessage = event.getMessage().getRawContent().split(" ");
        String command = splitMessage[0].substring(this.prefix.length()).toLowerCase();
        if (command.toLowerCase().equals("help".toLowerCase())) {
            help(event);
        }
        for (Method method : this.methods) {
            if (method.getAnnotation(Command.class).value().toLowerCase().equals(command)) {
                try {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (splitMessage.length != paramTypes.length) {
                        return;
                    }
                    List<Object> objects = new ArrayList<>();
                    objects.add(event);
                    int x = 1;
                    while (x < paramTypes.length) {
                        if(paramTypes[x].equals(String.class)) {
                            objects.add(splitMessage[x]);
                            
                        }
                        else if(paramTypes[x].equals(Long.class)) {
                            objects.add(Long.parseLong(splitMessage[x]));
                        }
                        else if(paramTypes[x].equals(Boolean.class)) {
                            objects.add(Boolean.parseBoolean(splitMessage[x]));
                        }
                        else if(paramTypes[x].isEnum()) {
                            for(Object obj : paramTypes[x].getEnumConstants()) {
                                if(obj.toString().equalsIgnoreCase(splitMessage[x])) {
                                    objects.add(obj);
                                }
                            }
                        }
                        x++;
                    }
                    method.invoke(null, objects.toArray(new Object[objects.size()]));
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    event.getChannel().sendMessage("Syntax error.").queue();
                }
                return;
            }
        }
    }

    private void help(MessageReceivedEvent event) {
        int fieldCount = 0;
        final int maxCount = 10;
        EmbedBuilder builder = new EmbedBuilder();
        for (Method method : this.methods) {
            Field field;
            String title = this.prefix
                    + method.getAnnotation(Command.class).value()
                    + getParamsAsString(method);
            if (method.isAnnotationPresent(Description.class)) {

                field = new Field(title,
                        method.getAnnotation(Description.class).value(), false);
            }
            else {
                field = new Field(title, "", false);
            }
            builder.addField(field);

            fieldCount++;
            if (fieldCount == maxCount) {
                fieldCount = 0;
                event.getChannel().sendMessage(builder.build()).queue();
                builder.clearFields();
            }
        }
        if (!builder.getFields().isEmpty()) {
            event.getChannel().sendMessage(builder.build()).queue();
        }
    }

    private String getParamsAsString(Method method) {
        StringBuilder paramstring = new StringBuilder();
        java.lang.reflect.Parameter[] params = method.getParameters();
        if (params.length == 1) {
            return paramstring.toString();
        }
        for (int x = 1; x < params.length; x++) {
            paramstring.append(" ");
            if (params[x].isAnnotationPresent(Parameter.class)) {
                paramstring.append('[');
                paramstring.append(params[x].getAnnotation(Parameter.class).value());
                paramstring.append(']');
            }
            else {
                paramstring.append('[');
                paramstring.append(params[x].getType().getName());
                paramstring.append(' ');
                paramstring.append(params[x].getName());
                paramstring.append(']');
            }
        }

        return paramstring.toString();
    }

    public Collection<Method> getCommandMethods() {
        return this.methods;
    }

}
