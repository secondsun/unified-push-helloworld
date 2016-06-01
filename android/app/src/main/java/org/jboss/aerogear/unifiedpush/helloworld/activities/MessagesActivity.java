/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.helloworld.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.jboss.aerogear.android.core.Callback;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.gcm.AeroGearGCMPushJsonConfiguration;
import org.jboss.aerogear.android.unifiedpush.gcm.UnifiedPushMessage;
import org.jboss.aerogear.android.unifiedpush.metrics.UnifiedPushMetricsMessage;
import org.jboss.aerogear.unifiedpush.helloworld.HelloWorldApplication;
import org.jboss.aerogear.unifiedpush.helloworld.R;
import org.jboss.aerogear.unifiedpush.helloworld.callback.MetricsCallback;
import org.jboss.aerogear.unifiedpush.helloworld.handler.NotificationBarMessageHandler;

import static org.jboss.aerogear.unifiedpush.helloworld.HelloWorldApplication.PUSH_REGISTER_NAME;

public class MessagesActivity extends AppCompatActivity implements MessageHandler {

    private HelloWorldApplication application;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages);

        application = (HelloWorldApplication) getApplication();

        if(getIntent().getBooleanExtra(HelloWorldApplication.PUSH_MESSAGE_FROM_BACKGROUND, false)) {
            UnifiedPushMetricsMessage metricsMessage = new UnifiedPushMetricsMessage(getIntent().getExtras());
            application.sendMetric(metricsMessage, new MetricsCallback());
        }

        View emptyView = findViewById(R.id.empty);
        listView = (ListView) findViewById(R.id.messages);
        listView.setEmptyView(emptyView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.topic_1:
            case R.id.topic_2:
            case R.id.topic_3:
                subscribeToCategory(item.getTitle());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void subscribeToCategory(CharSequence title) {
        RegistrarManager.config(PUSH_REGISTER_NAME, AeroGearGCMPushJsonConfiguration.class)
                .loadConfigJson(getApplicationContext())
                .addCategory(title.toString())
                .asRegistrar();

        PushRegistrar registrar = RegistrarManager.getRegistrar(PUSH_REGISTER_NAME);
        registrar.register(getApplicationContext(), new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getString(R.string.registration_successful),
                        Toast.LENGTH_LONG).show();

            }

            @Override
            public void onFailure(Exception e) {
                Log.e("", e.getMessage());
                Toast.makeText(getApplicationContext(),
                        getApplication().getString(R.string.registration_error),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RegistrarManager.registerMainThreadHandler(this);
        RegistrarManager.unregisterBackgroundThreadHandler(NotificationBarMessageHandler.instance);

        displayMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        RegistrarManager.unregisterMainThreadHandler(this);
        RegistrarManager.registerBackgroundThreadHandler(NotificationBarMessageHandler.instance);
    }

    @Override
    public void onMessage(Context context, Bundle bundle) {
        addNewMessage(bundle);
    }

    private void addNewMessage(Bundle bundle) {
        String message = bundle.getString(UnifiedPushMessage.ALERT_KEY);
        application.addMessage(message);
        displayMessages();
    }

    private void displayMessages() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
            R.layout.message_item, application.getMessages());
        listView.setAdapter(adapter);
    }
}
