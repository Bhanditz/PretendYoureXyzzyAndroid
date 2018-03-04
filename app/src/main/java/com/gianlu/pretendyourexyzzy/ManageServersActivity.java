package com.gianlu.pretendyourexyzzy;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;

import org.json.JSONException;

import java.util.Objects;

import okhttp3.HttpUrl;

public class ManageServersActivity extends AppCompatActivity implements ServersAdapter.IAdapter {
    private RecyclerViewLayout layout;
    private ServersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(R.string.manageServers);

        layout.disableSwipeRefresh();
        layout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        loadServers();
    }

    private void loadServers() {
        adapter = new ServersAdapter(this, PYX.Server.loadUserServers(this), this);
        layout.loadListData(adapter, false);
    }

    @SuppressLint("InflateParams")
    private void addServer(@Nullable final PYX.Server server) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_add_server, null, false);

        final TextInputLayout nameField = layout.findViewById(R.id.addServer_name);
        nameField.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                nameField.setErrorEnabled(false);
            }
        });
        final TextInputLayout urlField = layout.findViewById(R.id.addServer_url);
        urlField.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                urlField.setErrorEnabled(false);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.addServer)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(server == null ? R.string.add : R.string.apply, null);

        if (server != null) {
            nameField.getEditText().setText(server.name);
            urlField.getEditText().setText(server.url.toString());
            builder.setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PYX.Server.removeServer(ManageServersActivity.this, server);
                    adapter.notifyItemRemoved(server);
                }
            });
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String nameStr = nameField.getEditText().getText().toString();
                        if (nameStr.isEmpty() || (server != null && !Objects.equals(server.name, nameStr) && PYX.Server.hasServer(ManageServersActivity.this, nameStr))) {
                            nameField.setError(getString(R.string.invalidServerName));
                            return;
                        }

                        String urlStr = urlField.getEditText().getText().toString();
                        HttpUrl url = PYX.Server.parseUrl(urlStr);
                        if (url == null) {
                            urlField.setError(getString(R.string.invalidServerUrl));
                            return;
                        }

                        PYX.Server server = new PYX.Server(url, nameStr);
                        try {
                            PYX.Server.addServer(ManageServersActivity.this, server);
                            loadServers();
                        } catch (JSONException ex) {
                            Toaster.show(ManageServersActivity.this, Utils.Messages.FAILED_ADDING_SERVER, ex);
                        }

                        dialogInterface.dismiss();
                    }
                });
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                adapter.startTests();
            }
        });

        CommonUtils.showDialog(this, dialog);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_servers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manageServers_add:
                addServer(null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) layout.showMessage(R.string.noServers, false);
        else layout.showList();
    }

    @Override
    public void serverSelected(PYX.Server server) {
        addServer(server);
    }
}
