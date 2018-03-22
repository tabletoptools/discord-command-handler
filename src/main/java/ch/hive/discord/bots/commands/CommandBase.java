/*
 * Copyright Carlo Field (cfi@bluesky-it.ch)
 */
package ch.hive.discord.bots.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kileraptor1
 */
public class CommandBase {

    private static CommandBase instance;
    private final Collection<Method> methods;
    private String prefix = "";
    private Color color;

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

    public CommandBase setColor(Color color) {
        this.color = color;
        return this;
    }

    public void process(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith(this.prefix)) {
            return;
        }

        String[] splitMessage = event.getMessage().getContentRaw().split(" ");
        String command = splitMessage[0].substring(this.prefix.length()).toLowerCase();
        if (command.toLowerCase().equals("help".toLowerCase())) {
            help(event);
        }
        for (Method method : this.methods) {
            if (method.getAnnotation(Command.class).value().toLowerCase().equals(command)) {
                if (method.isAnnotationPresent(Constraint.class)) {
                    if (applyConstraints(event, method)) return;
                }
                try {
                    invokeMethod(event, splitMessage, method);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    event.getChannel().sendMessage("Syntax error.").queue();
                }
                return;
            }
        }
    }

    private boolean applyConstraints(MessageReceivedEvent event, Method method) {
        Constraint constraintAnnotation = method.getAnnotation(Constraint.class);
        Boolean succeededCheck = false;
        for (Class c : constraintAnnotation.value()) {
            if (!ConstraintCheck.class.isAssignableFrom(c)) {
                Loggers.COMMANDLOGGER.warn("Class {} does not extend ConstraintCheck. Silently failing but not checking.", c.getName());
            } else {
                try {
                    ConstraintCheck check = (ConstraintCheck) c.newInstance();
                    Boolean result = check.check(event);
                    if (!result && constraintAnnotation.enforceAll()) {
                        succeededCheck = false;
                        break;
                    }
                    else if(result && constraintAnnotation.enforceAll()) {
                        succeededCheck = true;
                    }
                    else if(result) {
                        succeededCheck = true;
                        if (!constraintAnnotation.enforceAll()) break;
                    }
                } catch (InstantiationException | IllegalAccessException ex) {
                    Loggers.COMMANDLOGGER.warn("Can't instantiate class {}. Silently failing.", c.getName());
                }
            }
        }
        if (!succeededCheck) {
            StringBuilder builder = new StringBuilder()
                    .append("We found some issues.\n");
            if(constraintAnnotation.enforceAll()) {
                builder.append("You need to fix all of these errors before issuing this command again:\n");
            }
            else {
                builder.append("You need to fix one of these errors before issuing this command again:\n");
            }
            for (Class c : constraintAnnotation.value()) {
                if (!ConstraintCheck.class.isAssignableFrom(c)) {
                    Loggers.COMMANDLOGGER.warn("Class {} does not extend ConstraintCheck. Silently failing but not checking.", c.getName());
                } else {
                    try {
                        ConstraintCheck check = (ConstraintCheck) c.newInstance();
                        builder.append(check.errorMessage())
                                .append("\n");

                    } catch (InstantiationException | IllegalAccessException ex) {
                        Loggers.COMMANDLOGGER.warn("Can't instantiate class {}. Silently failing. Again. This is an issue caused by the implementing application. Make sure your constraint extends ConstraintCheck.", c.getName());
                    }
                }
            }
            event.getChannel().sendMessage(builder).queue();
            return true;
        }
        return false;
    }

    private void invokeMethod(MessageReceivedEvent event, String[] splitMessage, Method method) throws IllegalAccessException, InvocationTargetException {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (splitMessage.length < paramTypes.length) {
            event.getChannel().sendMessage("Error: Invalid Argument Count.").queue();
            return;
        }
        List<Object> objects = new ArrayList<>();
        objects.add(event);
        int x = 1;
        while (x < splitMessage.length) {
            try {
                if (paramTypes[x].equals(String.class)) {
                    objects.add(splitMessage[x]);
                } else if (paramTypes[x].equals(Long.class)) {
                    objects.add(Long.parseLong(splitMessage[x]));
                } else if (paramTypes[x].equals(Boolean.class)) {
                    objects.add(Boolean.parseBoolean(splitMessage[x]));
                } else if (paramTypes[x].equals(Integer.class)) {
                    objects.add(Integer.parseInt(splitMessage[x]));
                } else if (paramTypes[x].isEnum()) {
                    for (Object obj : paramTypes[x].getEnumConstants()) {
                        if (obj.toString().equalsIgnoreCase(splitMessage[x])) {
                            objects.add(obj);
                        }
                    }
                } else if (paramTypes[x].isArray()) {
                    List<String> list = new ArrayList<>();
                    list.add(splitMessage[x]);
                    objects.add(list);
                }
            }
            catch(IndexOutOfBoundsException ex) {
                if(List.class.isAssignableFrom(objects.get(objects.size()-1).getClass())) {
                    ((ArrayList<String>)objects.get(objects.size()-1)).add(splitMessage[x]);
                }
            }
            finally {
                x++;
            }
        }
        if(List.class.isAssignableFrom(objects.get(objects.size()-1).getClass())) {
            List<Object> list = (List<Object>)objects.get(objects.size()-1);
            List<String> strings = list.stream()
                    .map(object -> Objects.toString(object, null))
                    .collect(Collectors.toList());
            String[] stringsAsArray = Arrays.copyOf(strings.toArray(), strings.size(), String[].class);
            objects.remove(objects.size()-1);
            objects.add(stringsAsArray);
        }
        method.invoke(null, objects.toArray(new Object[objects.size()]));
    }

    private void help(MessageReceivedEvent event) {
        event.getMessage().delete().queue();
        int fieldCount = 0;
        final int maxCount = 10;
        EmbedBuilder builder = new EmbedBuilder();
        if(color != null) {
            builder.setColor(color);
        }
        for (Method method : this.methods) {
            Field field;
            String title = this.prefix
                    + method.getAnnotation(Command.class).value()
                    + getParamsAsString(method);
            if (method.isAnnotationPresent(Description.class)) {

                field = new Field(title,
                        method.getAnnotation(Description.class).value(), false);
            } else {
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
            } else {
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
