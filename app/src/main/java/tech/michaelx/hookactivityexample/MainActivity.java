package tech.michaelx.hookactivityexample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnClicked();
            }
        });
    }

    private void onBtnClicked() {
        // 直接启动未注册的Activity，需要做动态代理，hook内存中的一些单例对象
        Intent intent = new Intent(this, OtherActivity.class);
        startActivity(intent);
    }
}
