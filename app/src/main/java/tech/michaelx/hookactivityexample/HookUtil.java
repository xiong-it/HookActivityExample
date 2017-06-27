package tech.michaelx.hookactivityexample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by michaelx on 2017/6/27.
 * <p>
 * Blog:http://blog.csdn.net/xiong_it | https://xiong-it.github.io
 * github:https://github.com/xiong-it
 * <p>
 * Hook工具类
 * <p>
 * 关键点：反射，静态，单例。
 */

public class HookUtil {
    private static final String TAG = "HookUtil";

    private Context mContext;
    private Class<? extends Activity> mProxyActivity;

    private static final String ACTIVITY_MANAGER_NATIVE_CLASS_NAME = "android.app.ActivityManagerNative";
    private static final String SINGLETON_CLASS_NAME = "android.util.Singleton";
    private static final String IACTIVITY_MANAGER_CLASS_NAME = "android.app.IActivityManager";
    private static final String G_DEFAULT_FIELD_NAME = "gDefault";
    private static final String M_INSTANCE_FIELD_NAME = "mInstance";

    private static final String ACTIVITY_THREAD_CLASS_NAME = "android.app.ActivityThread";
    private static final String CURRENT_ACTIVITY_THREAD_METHOD_NAME = "currentActivityThread";
    private static final String M_H_FIELD_NAME = "mH";
    private static final String M_CALLBACK_FIELD_NAME = "mCallback";
    private static final String INTENT_FIELD_NAME = "intent";

    private static final String HOOK_FUNCTION_NAME = "startActivity";

    private static final String INTENT_EXTRA_PARAM = "intent_extra";

    private static final int LAUNCH_ACTIVITY = 100; // 100为启动Activity消息,定义在 ActivityThread.H 中

    public HookUtil(Context context, Class<? extends Activity> proxyActivity) {
        mContext = context;
        mProxyActivity = proxyActivity;
    }

    public void hookAms() {
        try {
            // 反射ActivityManagerNative
            Class<?> amnClazz = Class.forName(ACTIVITY_MANAGER_NATIVE_CLASS_NAME);
            // ActivityManagerNative的全局static final变量Singleton gDefault
            Field gDefaultField = amnClazz.getDeclaredField(G_DEFAULT_FIELD_NAME);
            gDefaultField.setAccessible(true);
            // 得到Singleton对象gDefault
            Object gDefaultObj = gDefaultField.get(null);

            // 反射Singleton
            Class<?> singletonClazz = Class.forName(SINGLETON_CLASS_NAME);
            Field mInstanceField = singletonClazz.getDeclaredField(M_INSTANCE_FIELD_NAME);
            mInstanceField.setAccessible(true);
            // Singleton的全局private变量mInstance(IActivityManager对象)
            Object iamgrObj = mInstanceField.get(gDefaultObj);

            // 开始替换真实的IActivityManager对象
            Class<?> iAtyMgrIntercept = Class.forName(IACTIVITY_MANAGER_CLASS_NAME);
            InvocationHandler amsInvocationHandler = new AmsInvocationHandler(iamgrObj);
            // 内存中IActivityManager对象的动态代理对象
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{iAtyMgrIntercept},
                    amsInvocationHandler);

            // 对IActivityManager对象进行替换
            mInstanceField.set(gDefaultObj, proxy);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void hookSystemHandler() {
        try {
            Class<?> aTreadClazz = Class.forName(ACTIVITY_THREAD_CLASS_NAME);
            Method currentAThreadMethod = aTreadClazz.getDeclaredMethod(CURRENT_ACTIVITY_THREAD_METHOD_NAME);
            currentAThreadMethod.setAccessible(true);
            // 获取到sCurrentActivityThread
            Object aThreadObj = currentAThreadMethod.invoke(null);
            // 利用sCurrentActivityThread去得到mH（Handler对象）
            Field mHField = aTreadClazz.getDeclaredField(M_H_FIELD_NAME);
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(aThreadObj);
            // 利用CallBack拦截Handler消息
            Field mCallbackField = Handler.class.getDeclaredField(M_CALLBACK_FIELD_NAME);
            mCallbackField.setAccessible(true);
            mCallbackField.set(mH, new AtyThreadHandlerCallback(mH));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    class AmsInvocationHandler implements InvocationHandler {
        private Object mActualObj;

        public AmsInvocationHandler(Object actualObj) {
            mActualObj = actualObj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "funName = " + method.getName());
            String funcName = method.getName();
            if (HOOK_FUNCTION_NAME.equals(funcName)) {
                Intent intent = new Intent();
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        intent = (Intent) args[i];
                        index = i;
                    }
                }

                Intent proxyIntent = new Intent();
                // 将intent替换为一个可以通过检测的Intent
                ComponentName componentName = new ComponentName(mContext, mProxyActivity);
                proxyIntent.setComponent(componentName);
                proxyIntent.putExtra(INTENT_EXTRA_PARAM, intent);
                args[index] = proxyIntent;
            }

            return method.invoke(mActualObj, args);
        }
    }

    class AtyThreadHandlerCallback implements Handler.Callback {
        private Handler mHandler;

        public AtyThreadHandlerCallback(Handler handler) {
            mHandler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "AtyThreadHandlerCallback--->handleMessage");
            if (msg.what == LAUNCH_ACTIVITY) {
                handleLaunchActivityMsg(msg);
            }
            mHandler.handleMessage(msg);
            return true;
        }
    }

    private void handleLaunchActivityMsg(Message msg) {
        // 获取ActivityClientRecord对象
        Object acrObj = msg.obj;
        try {
            // 得到ActivityClientRecord对象中的intent变量
            Field intenField = acrObj.getClass().getDeclaredField(INTENT_FIELD_NAME);
            intenField.setAccessible(true);
            // 得到ActivityClientRecord对象中的intent对象
            Intent proxyIntent = (Intent) intenField.get(acrObj);
            Intent actualIntent = proxyIntent.getParcelableExtra(INTENT_EXTRA_PARAM);
            if (actualIntent != null) {
                // 将intent替换回来
                proxyIntent.setComponent(actualIntent.getComponent());
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
