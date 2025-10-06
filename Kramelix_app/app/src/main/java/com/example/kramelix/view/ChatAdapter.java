package com.example.kramelix.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kramelix.R;
import com.example.kramelix.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_USER = 1;
    private static final int VIEW_ASSISTANT = 2;
    private final List<Message> data = new ArrayList<>();

    public void submit(List<Message> msgs) {
        data.clear();
        if (msgs != null) data.addAll(msgs);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        Message m = data.get(position);
        return (m.getRole() == Message.Role.USER) ? VIEW_USER : VIEW_ASSISTANT;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v = inf.inflate(
                viewType == VIEW_USER ? R.layout.item_message_user : R.layout.item_message_assistant,
                parent, false
        );
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        MsgVH vh = (MsgVH) holder;
        Message m = data.get(pos);

        // Always reset recycled view to a known state first
        vh.stopPulse();
        vh.stopDots(null);

        if (m.isPending()) {
            vh.text.setAlpha(1f);           // base
            vh.text.setText("…");           // default seed
            // Start animations
            vh.startPulse();
            vh.startDots(m.getText());      // if you seeded "…" or custom text
        } else {
            vh.text.setAlpha(1f);
            vh.text.setText(m.getText());
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView text;

        private ObjectAnimator pulse;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable dotsRunnable;

        MsgVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
        }

        void startPulse() {
            if (pulse == null) {
                pulse = ObjectAnimator.ofFloat(text, "alpha", 0.4f, 1.0f);
                pulse.setDuration(700);
                pulse.setInterpolator(new LinearInterpolator());
                pulse.setRepeatMode(ValueAnimator.REVERSE);
                pulse.setRepeatCount(ValueAnimator.INFINITE);
            }
            if (!pulse.isStarted()) pulse.start();
        }

        void stopPulse() {
            if (pulse != null && pulse.isStarted()) pulse.cancel();
            text.setAlpha(1f);
        }

        void startDots(String startWith) {
            if (dotsRunnable != null) return;
            text.setText((startWith == null || startWith.isBlank()) ? "…" : startWith);

            dotsRunnable = new Runnable() {
                int i = 0;
                @Override public void run() {
                    int count = (i % 3) + 1; // 1..3
                    String dots = new String(new char[count]).replace('\0', '.');
                    text.setText(dots);
                    i++;
                    handler.postDelayed(this, 350);
                }
            };
            handler.post(dotsRunnable);
        }

        void stopDots(String finalText) {
            if (dotsRunnable != null) {
                handler.removeCallbacks(dotsRunnable);
                dotsRunnable = null;
            }
            if (finalText != null) {
                text.setText(finalText.isBlank() ? "[no response given]" : finalText);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        MsgVH vh = (MsgVH) holder;
        vh.stopPulse();
        vh.stopDots(null);
        super.onViewRecycled(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        MsgVH vh = (MsgVH) holder;
        vh.stopPulse();
        super.onViewDetachedFromWindow(holder);
    }
}
