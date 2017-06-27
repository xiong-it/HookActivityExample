package tech.michaelx.hookactivityexample;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by michaelx on 2017/6/27.
 * <p>
 * 被代理的Activity，未在清单中注册
 * <p>
 * Blog:http://blog.csdn.net/xiong_it | https://xiong-it.github.io
 * github:https://github.com/xiong-it
 * <p>
 * 关键点：不要使用{@link AppCompatActivity}作为父类，代理会被系统拦截，导致崩溃！
 */

public class OtherActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("我没有在AndroidMenifest清单注册，嘿哈！");

        setContentView(textView);
    }
}
