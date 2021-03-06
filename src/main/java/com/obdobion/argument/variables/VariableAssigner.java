package com.obdobion.argument.variables;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import com.obdobion.argument.CmdLine;
import com.obdobion.argument.type.CmdLineCLA;
import com.obdobion.argument.type.EnumCLA;
import com.obdobion.argument.type.ICmdLineArg;

/**
 * <p>
 * VariableAssigner class.
 * </p>
 *
 * @author Chris DeGreef fedupforone@gmail.com
 */
public class VariableAssigner implements IVariableAssigner
{
    static private IVariableAssigner instance;

    static private void assign(final Field field, final ICmdLineArg<?> arg, final Object target)
            throws ParseException
    {
        if (arg.getVariable() == null)
            return;
        if (arg.getValue() == null)
            return;
        if (field == null)
            return;

        /*
         * Allows access to non-public fields.
         */
        field.setAccessible(true);

        if (!(arg instanceof CmdLineCLA) && arg.getFactoryMethodName() != null)
            assignWithInstantiator(field, arg, target);
        else
            assignStandard(field, arg, target);
    }

    /**
     * This value may not be appropriate for the list. But since this is at
     * runtime we can't make use of the generics definition they may have
     * provided on the field. If the value is not what they expect then they
     * need to provide a --factory set of methods.
     */
    private static void assignList(final Field field, final ICmdLineArg<?> arg, final Object target)
            throws IllegalAccessException
    {
        @SuppressWarnings("unchecked")
        Collection<Object> alist = (Collection<Object>) field.get(target);

        if (alist == null)
        {
            alist = new ArrayList<>();
            field.set(target, alist);
        }
        for (int v = 0; v < arg.size(); v++)
            alist.add(arg.getDelegateOrValue(v));
    }

    static private void assignStandard(final Field field, final ICmdLineArg<?> arg, final Object target)
            throws ParseException
    {
        final String errMsg = "expected: public "
                + arg.getValue().getClass().getName()
                + " "
                + arg.getVariable()
                + " on "
                + target.getClass().getName();
        try
        {
            if (isStringArray(field))
                field.set(target, arg.getValueAsStringArray());
            else if (isIntegerArray(field))
                field.set(target, arg.getValueAsIntegerArray());
            else if (isintArray(field))
                field.set(target, arg.getValueAsintArray());
            else if (isDoubleArray(field))
                field.set(target, arg.getValueAsDoubleArray());
            else if (isdoubleArray(field))
                field.set(target, arg.getValueAsdoubleArray());
            else if (isLongArray(field))
                field.set(target, arg.getValueAsLongArray());
            else if (islongArray(field))
                field.set(target, arg.getValueAslongArray());
            else if (isbyteArray(field))
                field.set(target, arg.getValueAsbyteArray());
            else if (ischarArray(field))
                field.set(target, arg.getValueAscharArray());
            else if (isfloatArray(field))
                field.set(target, arg.getValueAsfloatArray());
            else if (isFloatArray(field))
                field.set(target, arg.getValueAsFloatArray());
            else if (isPatternArray(field))
                field.set(target, arg.getValueAsPatternArray());
            else if (isPattern(field))
                field.set(target, arg.getValueAsPattern());
            else if (isDateTimeFormatterArray(field))
                field.set(target, arg.getValueAsDateTimeFormatterArray());
            else if (isDateTimeFormatter(field))
                field.set(target, arg.getValueAsDateTimeFormatter());
            else if (isSimpleDateFormatArray(field))
                field.set(target, arg.getValueAsSimpleDateFormatArray());
            else if (isSimpleDateFormat(field))
                field.set(target, arg.getValueAsSimpleDateFormat());
            else if (isEquationArray(field))
                field.set(target, arg.getValueAsEquationArray());
            else if (isEquation(field))
                field.set(target, arg.getValueAsEquation());
            else if (isDateArray(field))
                field.set(target, arg.getValueAsDateArray());
            else if (isCalendarArray(field))
                field.set(target, arg.getValueAsCalendarArray());
            else if (isLocalDateTimeArray(field))
                field.set(target, arg.getValueAsLocalDateTimeArray());
            else if (isLocalDateArray(field))
                field.set(target, arg.getValueAsLocalDateArray());
            else if (isLocalTimeArray(field))
                field.set(target, arg.getValueAsLocalTimeArray());
            else if (isByteArray(field))
                field.set(target, arg.getValueAsByteArray());
            else if (isCharacterArray(field))
                field.set(target, arg.getValueAsCharacterArray());
            else if (isFileArray(field))
                field.set(target, arg.getValueAsFileArray());
            else if (isEnum(field))
                field.set(target, arg.asEnum(field.getName(), field.getType().getEnumConstants()));
            else if (isEnumArray(field))
                field.set(target,
                        arg.asEnumArray(field.getName(), field.getType().getComponentType().getEnumConstants()));
            else if (isList(field))
                assignList(field, arg, target);
            else
                field.set(target, arg.getDelegateOrValue());
        } catch (final SecurityException e)
        {
            throw new ParseException("SecurityException: " + errMsg, -1);
        } catch (final IllegalArgumentException e)
        {
            throw new ParseException(e.toString() + ": " + errMsg, -1);
        } catch (final IllegalAccessException e)
        {
            throw new ParseException("IllegalAccessException: " + errMsg, -1);
        }

    }

    static private void assignWithInstantiator(final Field field, final ICmdLineArg<?> arg, final Object target)
            throws ParseException
    {
        Class<?> clazz = null;
        Method method = null;
        String baseClassName;
        if (field.getType().getName().charAt(0) == '[')
            baseClassName = field.getType().getName().substring(2, field.getType().getName().length() - 1);
        else if (arg.getInstanceClass() != null)
            baseClassName = arg.getInstanceClass();
        else
            baseClassName = field.getType().getName();

        String errMsg = null;

        try
        {
            final int methodPvt = arg.getFactoryMethodName().lastIndexOf('.');

            final Class<?> parmClass = arg.getValue(0).getClass();

            if (methodPvt < 0)
            {

                /*
                 * It is too bad that the generic info on a field is not public.
                 * So here we can't determine the generic type of a list. To add
                 * multi value items to a list the --class parameter must be
                 * used.
                 */
                /*-
                if (field.getType().getPackage().getName().startsWith("java."))
                    throw new ParseException("Generics with a factory method must use --class", 0);
                */

                errMsg = "expected: public static "
                        + baseClassName
                        + " "
                        + arg.getFactoryMethodName()
                        + "("
                        + parmClass.getName()
                        + ") on "
                        + baseClassName;
                clazz = CmdLine.ClassLoader.loadClass(baseClassName);
                method = clazz.getDeclaredMethod(arg.getFactoryMethodName(), parmClass);
            } else
            {
                errMsg = "expected: public static "
                        + arg.getFactoryMethodName().substring(0, methodPvt)
                        + " "
                        + arg.getFactoryMethodName().substring(methodPvt + 1)
                        + "("
                        + parmClass.getName()
                        + ") on "
                        + arg.getFactoryMethodName().substring(0, methodPvt);
                clazz = CmdLine.ClassLoader.loadClass(arg.getFactoryMethodName().substring(0, methodPvt));
                method = clazz.getDeclaredMethod(arg.getFactoryMethodName().substring(methodPvt + 1), parmClass);
            }

            if (arg.isMultiple())
            {
                if (field.getType().getName().charAt(0) == '[')
                    for (int r = 0; r < arg.size(); r++)
                    {
                        final Object[] array = newArray(target, field);
                        array[array.length - 1] = method.invoke(null, arg.getValue(r));
                    }
                else
                    for (int r = 0; r < arg.size(); r++)
                    {
                        final ArrayList<Object> arrayList = newList(target, field);
                        arrayList.add(method.invoke(null, arg.getValue(r)));
                    }
            } else
                field.set(target, method.invoke(null, arg.getValue()));

        } catch (final InstantiationException e)
        {
            throw new ParseException("ClassNotFoundException " + errMsg, -1);
        } catch (final ClassNotFoundException e)
        {
            throw new ParseException("ClassNotFoundException " + errMsg, -1);
        } catch (final InvocationTargetException e)
        {
            throw new ParseException("InvocationTargetException " + errMsg, -1);
        } catch (final IllegalAccessException e)
        {
            throw new ParseException("IllegalAccessException " + errMsg, -1);
        } catch (final IllegalArgumentException e)
        {
            e.printStackTrace();
            throw new ParseException("IllegalArgumentException " + errMsg, -1);
        } catch (final SecurityException e)
        {
            throw new ParseException("SecurityException " + errMsg, -1);
        } catch (final NoSuchMethodException e)
        {
            throw new ParseException("NoSuchMethodException " + errMsg, -1);
        }

    }

    static private String factoryArgValue(final ICmdLineArg<?> arg)
    {
        try
        {
            if (arg == null)
                return null;
            if (!arg.isParsed())
                return null;
            if (arg instanceof EnumCLA)
            {
                final Class<?> clazz = CmdLine.ClassLoader.loadClass(arg.getInstanceClass());
                final Object[] possibleConstants = clazz.getEnumConstants();
                return ((EnumCLA) arg).asEnum((String) arg.getValue(), possibleConstants).toString();
            }
            return (String) arg.getValue();
        } catch (final ClassNotFoundException | ParseException e)
        {
            e.printStackTrace();
            return (String) arg.getValue();
        }
    }

    /**
     * <p>
     * findFieldInAnyParentOrMyself.
     * </p>
     *
     * @param arg a {@link com.obdobion.argument.type.ICmdLineArg} object.
     * @param targetClass a {@link java.lang.Class} object.
     * @param errMsg a {@link java.lang.String} object.
     * @return a {@link java.lang.reflect.Field} object.
     * @throws java.text.ParseException if any.
     */
    static public Field findFieldInAnyParentOrMyself(
            final ICmdLineArg<?> arg,
            final Class<?> targetClass,
            final String errMsg)
                    throws ParseException
    {
        Field field = null;
        try
        {
            field = targetClass.getDeclaredField(arg.getVariable());
        } catch (final SecurityException e)
        {
            throw new ParseException("SecurityException " + errMsg, -1);
        } catch (final NoSuchFieldException e)
        {
            if (targetClass.getSuperclass() == null)
                throw new ParseException("NoSuchFieldException " + errMsg, -1);
            /*
             * recursive from here
             */
            return findFieldInAnyParentOrMyself(arg, targetClass.getSuperclass(), errMsg);
        }
        return field;
    }

    /**
     * <p>
     * Getter for the field <code>instance</code>.
     * </p>
     *
     * @return a {@link com.obdobion.argument.variables.IVariableAssigner}
     *         object.
     */
    static public IVariableAssigner getInstance()
    {
        if (instance == null)
            instance = new VariableAssigner();
        return instance;
    }

    private static boolean isbyteArray(final Field field)
    {
        return "[B".equals(field.getType().getName());
    }

    private static boolean isByteArray(final Field field)
    {
        return "[Ljava.lang.Byte;".equals(field.getType().getName());
    }

    private static boolean isCalendarArray(final Field field)
    {
        return "[Ljava.util.Calendar;".equals(field.getType().getName());
    }

    private static boolean isCharacterArray(final Field field)
    {
        return "[Ljava.lang.Character;".equals(field.getType().getName());
    }

    private static boolean ischarArray(final Field field)
    {
        return "[C".equals(field.getType().getName());
    }

    private static boolean isDateArray(final Field field)
    {
        return "[Ljava.util.Date;".equals(field.getType().getName());
    }

    private static boolean isDateTimeFormatter(final Field field)
    {
        return "java.time.format.DateTimeFormatter".equals(field.getType().getName());
    }

    private static boolean isDateTimeFormatterArray(final Field field)
    {
        return "[Ljava.time.format.DateTimeFormatter;".equals(field.getType().getName());
    }

    private static boolean isdoubleArray(final Field field)
    {
        return "[D".equals(field.getType().getName());
    }

    private static boolean isDoubleArray(final Field field)
    {
        return "[Ljava.lang.Double;".equals(field.getType().getName());
    }

    static private boolean isEnum(final Field field)
    {
        return field.getType().isEnum();
    }

    static private boolean isEnumArray(final Field field)
    {
        return field.getType().isArray() && field.getType().getComponentType().isEnum();
    }

    private static boolean isEquation(final Field field)
    {
        return "com.obdobion.algebrain.Equ".equals(field.getType().getName());
    }

    private static boolean isEquationArray(final Field field)
    {
        return "[Lcom.obdobion.algebrain.Equ;".equals(field.getType().getName());
    }

    private static boolean isFileArray(final Field field)
    {
        return "[Ljava.io.File;".equals(field.getType().getName());
    }

    private static boolean isfloatArray(final Field field)
    {
        return "[F".equals(field.getType().getName());
    }

    private static boolean isFloatArray(final Field field)
    {
        return "[Ljava.lang.Float;".equals(field.getType().getName());
    }

    private static boolean isintArray(final Field field)
    {
        return "[I".equals(field.getType().getName());
    }

    private static boolean isIntegerArray(final Field field)
    {
        return "[Ljava.lang.Integer;".equals(field.getType().getName());
    }

    static private boolean isList(final Field field)
    {
        final Class<?>[] interfaces = field.getType().getInterfaces();
        if (interfaces.length == 0)
            return false;
        for (final Class<?> iface : interfaces)
            if (Collection.class.getName().equals(iface.getName()))
                return true;
        return false;
    }

    private static boolean isLocalDateArray(final Field field)
    {
        return "[Ljava.time.LocalDate;".equals(field.getType().getName());
    }

    private static boolean isLocalDateTimeArray(final Field field)
    {
        return "[Ljava.time.LocalDateTime;".equals(field.getType().getName());
    }

    private static boolean isLocalTimeArray(final Field field)
    {
        return "[Ljava.time.LocalTime;".equals(field.getType().getName());
    }

    private static boolean islongArray(final Field field)
    {
        return "[J".equals(field.getType().getName());
    }

    private static boolean isLongArray(final Field field)
    {
        return "[Ljava.lang.Long;".equals(field.getType().getName());
    }

    private static boolean isPattern(final Field field)
    {
        return "java.util.regex.Pattern".equals(field.getType().getName());
    }

    private static boolean isPatternArray(final Field field)
    {
        return "[Ljava.util.regex.Pattern;".equals(field.getType().getName());
    }

    private static boolean isSimpleDateFormat(final Field field)
    {
        return "java.text.SimpleDateFormat".equals(field.getType().getName());
    }

    private static boolean isSimpleDateFormatArray(final Field field)
    {
        return "[Ljava.text.SimpleDateFormat;".equals(field.getType().getName());
    }

    private static boolean isStringArray(final Field field)
    {
        return "[Ljava.lang.String;".equals(field.getType().getName());
    }

    /**
     * @param target
     * @param field
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    static private Object[] newArray(final Object target, final Field field)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        final Object[] oldinstance = (Object[]) field.get(target);
        int oldsize = 0;
        if (oldinstance != null)
            oldsize = oldinstance.length;

        final Object[] arrayinstance = (Object[]) Array.newInstance(field.getType().getComponentType(), oldsize + 1);

        int i = 0;
        if (oldinstance != null)
            for (; i < oldsize; i++)
                arrayinstance[i] = oldinstance[i];
        field.set(target, arrayinstance);
        return arrayinstance;
    }

    /**
     * @param group
     * @param target
     * @param field
     * @param baseClassName
     * @param factoryValueArg
     * @param reusable
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ParseException
     */
    static private Object newInstanceForGroup(
            final CmdLineCLA group,
            final Object target,
            final Field field,
            final String _baseClassName,
            final ICmdLineArg<?> factoryValueArg,
            final boolean reusable)
                    throws ClassNotFoundException,
                    InstantiationException,
                    IllegalAccessException,
                    SecurityException,
                    NoSuchMethodException,
                    IllegalArgumentException,
                    InvocationTargetException,
                    ParseException
    {
        String baseClassName = _baseClassName;

        Object groupInstance;
        if (baseClassName == null)
            if (group.getInstanceClass() != null)
                baseClassName = group.getInstanceClass();
            else
                baseClassName = field.getType().getName();

        /*
         * Allow an instantiated instance variable to be used rather than
         * replaced.
         */
        if (reusable && field.get(target) != null)
        {
            final Object value = field.get(target);
            if (!value.getClass().getName().equals(baseClassName))
                throw new ParseException("Error in instance creation for \"" + group.toString() + "\", "
                        + value.getClass().getName() + " can not be reassigned to " + baseClassName, 0);
            return value;
        }

        Class<?> clazz;
        Method method;

        /*
         * Get the proper constructor and possible argument to create the
         * instance
         */
        if (group.getFactoryMethodName() != null)
        {
            final int methodPvt = group.getFactoryMethodName().lastIndexOf('.');
            final String factoryValue = factoryArgValue(factoryValueArg);
            if (methodPvt < 0)
            {
                clazz = CmdLine.ClassLoader.loadClass(baseClassName);
                if (factoryValue == null)
                    method = clazz.getDeclaredMethod(group.getFactoryMethodName());
                else
                    method = clazz.getDeclaredMethod(group.getFactoryMethodName(), String.class);
            } else
            {
                clazz = CmdLine.ClassLoader.loadClass(group.getFactoryMethodName().substring(0, methodPvt));
                if (factoryValue == null)
                    method = clazz.getDeclaredMethod(group.getFactoryMethodName().substring(methodPvt + 1));
                else
                    method = clazz
                            .getDeclaredMethod(group.getFactoryMethodName().substring(methodPvt + 1), String.class);
            }
            if (factoryValue == null)
                groupInstance = method.invoke(clazz, new Object[] {});
            else
                groupInstance = method.invoke(clazz, new Object[] {
                        factoryValue
                });

        } else
        {
            clazz = CmdLine.ClassLoader.loadClass(baseClassName);
            groupInstance = clazz.newInstance();
        }
        return groupInstance;
    }

    /**
     * @param target
     * @param field
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    static private ArrayList<Object> newList(final Object target, final Field field)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        @SuppressWarnings("unchecked")
        ArrayList<Object> oldinstance = (ArrayList<Object>) field.get(target);

        if (oldinstance == null)
        {
            oldinstance = new ArrayList<>();
            field.set(target, oldinstance);
        }
        return oldinstance;
    }

    /**
     * <p>
     * Setter for the field <code>instance</code>.
     * </p>
     *
     * @param newInstance a
     *            {@link com.obdobion.argument.variables.IVariableAssigner}
     *            object.
     * @return a {@link com.obdobion.argument.variables.IVariableAssigner}
     *         object.
     */
    static public IVariableAssigner setInstance(final IVariableAssigner newInstance)
    {
        final IVariableAssigner previousAssigner = instance;
        instance = newInstance;
        return previousAssigner;
    }

    /** {@inheritDoc} */
    @Override
    public void assign(final ICmdLineArg<?> arg, final Object target) throws ParseException
    {
        if (arg == null)
            return;
        if (target == null)
            return;

        final String errMsg = "expected: "
                + arg.getValue().getClass().getName()
                + " "
                + arg.getVariable()
                + " on "
                + target.getClass().getName();

        final Field field = findFieldInAnyParentOrMyself(arg, target.getClass(), errMsg);
        assign(field, arg, target);
    }

    /** {@inheritDoc} */
    @Override
    public Object newGroupVariable(final CmdLineCLA group, final Object target, final ICmdLineArg<?> factoryValueArg)
            throws ParseException
    {
        try
        {
            if (group.getVariable() == null)
                return null;
            if (target == null)
                return null;

            final Field field = target.getClass().getDeclaredField(group.getVariable());
            /*
             * Allows access to non-public fields.
             */
            field.setAccessible(true);

            Object groupInstance = null;

            if (group.isMultiple())
            {
                String baseClassName;
                if (group.getInstanceClass() != null)
                {
                    if (field.getType().getName().charAt(0) == '[')
                    {
                        baseClassName = group.getInstanceClass();
                        final Object[] array = newArray(target, field);
                        array[array.length - 1] = newInstanceForGroup(group, target, field, baseClassName,
                                factoryValueArg, false);
                        groupInstance = array[array.length - 1];
                    } else
                    {
                        final ArrayList<Object> arrayList = newList(target, field);
                        groupInstance = newInstanceForGroup(group, target, field, null, factoryValueArg, false);
                        arrayList.add(groupInstance);
                    }
                } else if (field.getType().getName().charAt(0) == '[')
                {
                    baseClassName = field.getType().getName().substring(2, field.getType().getName().length() - 1);
                    final Object[] array = newArray(target, field);
                    array[array.length - 1] = newInstanceForGroup(group, target, field, baseClassName, factoryValueArg,
                            false);
                    groupInstance = array[array.length - 1];
                } else
                {
                    final ArrayList<Object> arrayList = newList(target, field);
                    groupInstance = newInstanceForGroup(group, target, field, null, factoryValueArg, false);
                    arrayList.add(groupInstance);
                }
            } else
            {
                groupInstance = newInstanceForGroup(group, target, field, null, factoryValueArg, true);
                field.set(target, groupInstance);
            }
            return groupInstance;
        } catch (final ClassNotFoundException e)
        {
            throw new ParseException("ClassNotFoundException (" + group.getVariable() + ")", -1);
        } catch (final InstantiationException e)
        {
            e.printStackTrace();
            throw new ParseException("InstantiationException (" + group.getVariable() + ")", -1);
        } catch (final IllegalAccessException e)
        {
            throw new ParseException("IllegalAccessException (" + group.getVariable() + ")", -1);
        } catch (final SecurityException e)
        {
            throw new ParseException("SecurityException (" + group.getVariable() + ")", -1);
        } catch (final NoSuchFieldException e)
        {
            throw new ParseException("NoSuchFieldException ("
                    + target.getClass().getSimpleName()
                    + " "
                    + group.getVariable()
                    + ")", -1);
        } catch (final IllegalArgumentException e)
        {
            throw new ParseException("IllegalArgumentException (" + group.getVariable() + ")", -1);
        } catch (final NoSuchMethodException e)
        {
            throw new ParseException("NoSuchMethodException ("
                    + target.getClass().getSimpleName()
                    + " "
                    + group.getVariable()
                    + " "
                    + group.getFactoryMethodName()
                    + ")", -1);
        } catch (final InvocationTargetException e)
        {
            throw new ParseException("InvocationTargetException (" + group.getVariable() + ")", -1);
        }
    }
}
