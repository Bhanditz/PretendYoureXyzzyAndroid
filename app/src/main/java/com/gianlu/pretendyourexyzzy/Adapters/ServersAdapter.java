package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.ServersChecker;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;
import java.util.Locale;

public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder> implements ServersChecker.OnResult {
    private final LayoutInflater inflater;
    private final List<PYX.Server> servers;
    private final ServersChecker checker;
    private final IAdapter listener;

    public ServersAdapter(Context context, List<PYX.Server> servers, IAdapter listener) {
        this.inflater = LayoutInflater.from(context);
        this.servers = servers;
        this.listener = listener;
        this.checker = new ServersChecker();

        listener.shouldUpdateItemCount(getItemCount());
        startTests();
    }

    public void startTests() {
        for (PYX.Server server : servers) checker.check(server, this);
        notifyDataSetChanged();
    }

    public void notifyItemRemoved(PYX.Server server) {
        int index = servers.indexOf(server);
        if (index != -1) {
            servers.remove(index);
            notifyItemRemoved(index);
            listener.shouldUpdateItemCount(getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final PYX.Server server = servers.get(position);
        holder.name.setText(server.name);
        holder.url.setText(server.url.toString());

        if (server.status == null) {
            holder.checking.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        } else {
            holder.checking.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);
            switch (server.status.status) {
                case ONLINE:
                    holder.statusIcon.setImageResource(R.drawable.ic_done_black_48dp);
                    holder.latency.setVisibility(View.VISIBLE);
                    holder.latency.setText(String.format(Locale.getDefault(), "%dms", server.status.latency));
                    break;
                case ERROR:
                    holder.statusIcon.setImageResource(R.drawable.ic_error_outline_black_48dp);
                    holder.latency.setVisibility(View.GONE);
                    break;
                case OFFLINE:
                    holder.statusIcon.setImageResource(R.drawable.ic_clear_black_48dp);
                    holder.latency.setVisibility(View.GONE);
                    break;
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.serverSelected(server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    @Override
    public void serverChecked(PYX.Server server) {
        int index = servers.indexOf(server);
        if (index != -1) notifyItemChanged(index);
    }

    public interface IAdapter {
        void shouldUpdateItemCount(int count);

        void serverSelected(PYX.Server server);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView url;
        final ProgressBar checking;
        final LinearLayout status;
        final ImageView statusIcon;
        final TextView latency;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_server, parent, false));

            name = itemView.findViewById(R.id.serverItem_name);
            url = itemView.findViewById(R.id.serverItem_url);
            checking = itemView.findViewById(R.id.serverItem_checking);
            status = itemView.findViewById(R.id.serverItem_status);
            statusIcon = status.findViewById(R.id.serverItem_statusIcon);
            latency = status.findViewById(R.id.serverItem_latency);
        }
    }
}