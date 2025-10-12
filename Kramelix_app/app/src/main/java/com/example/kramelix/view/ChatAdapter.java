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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kramelix.R;
import com.example.kramelix.model.Message;
import com.example.kramelix.model.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class represents the view for the adapter that binds {@link Message} items to chat row layouts for a RecyclerView.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // -------------------- VIEW TYPES ---------------------
    /**
     * The view type constant for USER bubbles.
     */
    private static final int VIEW_USER = 1;

    /**
     * The view type constant for ASSISTANT bubbles.
     */
    private static final int VIEW_ASSISTANT = 2;

    // -------------------- STATE ---------------------
    /**
     * Backing list for current messages in display order.
     * This is replaced via {@link #submit(Collection)} when the LiveData creates a new snapshot.
     */
    private final List<Message> data = new ArrayList<>(0);

    // -------------------- PUBLIC API ---------------------

    /**
     * This function replaces the current messages data with a new snapshot and refreshes the list.
     * AMY'S NOTE: This should be called on the main thread bc LiveData observers run on main by default.
     * @param msgs The new list of messages to display (null lists are treated as empty).
     */
    public final void submit(Collection<? extends Message> msgs) {
        data.clear(); // clearing old data

        if (null != msgs) data.addAll(msgs); // adding new data

        // FIXME OPTIMIZE: consider being more specific for smoother updates later
        notifyDataSetChanged(); // notify observers of full refresh
    }

    // -------------------- ADAPTER OVERRIDES ---------------------

    /**
     * A getter method for the view type of a given message.
     * @param position The position to query.
     * @return The view type of the message.
     */
    @Override
    public final int getItemViewType(int position) {

        // VARIABLE DECLARATION: retrieving the message
        Message m = data.get(position);

        // OUTPUT:
        return (Role.USER == m.getRole()) ? VIEW_USER : VIEW_ASSISTANT;

    }

    /**
     * This function creates a new ViewHolder for a given view type.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder.
     */
    @NonNull
    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // PROCESS: creating the view holder
        LayoutInflater inf = LayoutInflater.from(parent.getContext());

        // PROCESS: inflating the appropriate bubble layout, based on the role
        View v = inf.inflate(
                VIEW_USER == viewType ? R.layout.item_message_user : R.layout.item_message_assistant,
                parent, false
        );

        // OUTPUT:
        return new MsgVH(v);

    }

    /**
     * This function binds the data at the given position to the given ViewHolder.
     * @param holder The ViewHolder to be updated to represent the msg contents.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // AMY'S NOTE: We should always reset animations in this method to prevent recycled views from carrying old states.

        // VARIABLE DECLARATION: retrieving the message and its holder
        MsgVH vh = (MsgVH) holder;
        Message m = data.get(position);

        // PROCESS: resetting the view to avoid animation carry-overs
        vh.stopPulse();
        vh.stopDots();
        vh.text.setAlpha(1.0f);

        // PROCESS: binding the current state to the view
        if (m.isPending()) { // should show loading animation

            vh.text.setText("…");
            vh.startPulse();
            vh.startDots(m.getText()); // animating from '.' to '..' to '...'

        } else { // should show the actual text

            vh.text.setText(m.getText());

        }

    }

    /**
     * A getter method for the number of items in the message list.
     * @return The number of items in the list.
     */
    @Override
    public final int getItemCount() {
        return data.size();
    }

    // -------------------- VIEW HOLDER ---------------------

    /**
     * This class represents the view holder for a chat message.
     * It handles the animations for corresponding TextViews.
     */
    static class MsgVH extends RecyclerView.ViewHolder {

        /**
         * The chat bubble TextView (must be @+id/textMessage in both row layouts).
         */
        final TextView text;

        /**
         * The pulse animator.
         */
        private ObjectAnimator pulse;

        /**
         * A handler for the looping dots animation.
         */
        final Handler handler = new Handler(Looper.getMainLooper());

        /**
         * A runnable for the looping dots animation.
         */
        @Nullable
        private Runnable dotsRunnable;

        /**
         * Constructor for the view holder.
         * @param itemView The view to hold.
         */
        MsgVH(@NonNull View itemView) {

            // INITIALIZATION: setting the constructor content
            super(itemView);
            text = itemView.findViewById(R.id.textMessage); // binding the view

        }

        // -------------------- ANIMATION HELPERS ---------------------

        /**
         * This helper function starts the pulsing alpha animation if not already running.
         */
        final void startPulse() {

            // PROCESS: creating the animation
            if (null == pulse) { // doesn't already exist

                pulse = ObjectAnimator.ofFloat(text, "alpha", 0.4f, 1.0f);
                pulse.setDuration(700L);
                pulse.setInterpolator(new LinearInterpolator());
                pulse.setRepeatMode(ValueAnimator.REVERSE);
                pulse.setRepeatCount(ValueAnimator.INFINITE);

            }

            if (!pulse.isStarted()) pulse.start(); // starting the animation

        }

        /**
         * This helper function stops the pulsing animation & restore full opacity of the TextView.
         */
        final void stopPulse() {

            // PROCESS: stopping the animation
            if (null != pulse && pulse.isStarted()) pulse.cancel();
            text.setAlpha(1.0f); // restoring opacity

        }

        /**
         * This helper function starts a simple 'typing' dots loop (".", "..", "...").
         * @param startWith Optional text string to display as the loading animation. If null/blank, uses "…".
         */
        final void startDots(String startWith) {

            // PROCESS: stopping any existing animation
            if (null != dotsRunnable) return; // already animating, so exiting
            text.setText((null == startWith || startWith.isBlank()) ? "…" : startWith); // setting initial text

            // PROCESS: starting the animation
            dotsRunnable = new Runnable() {
                int i;

                /**
                 * This function runs the dots animation.
                 */
                @Override
                public void run() {

                    // VARIABLE DECLARATION:
                    int count = (i % 3) + 1; // cycles 1 ... 3
                    StringBuilder sb = new StringBuilder(0);

                    for (int k = 0; k < count; k++) sb.append('.');
                    text.setText(sb.toString());
                    i++; // updating counter

                    handler.postDelayed(this, 350L); // scheduling next run

                }

            };

            handler.post(dotsRunnable); // starting the animation

        }

        /**
         * This helper function stops the dots loop.
         */
        final void stopDots() {

            // PROCESS: stopping the animation
            if (null != dotsRunnable) {
                handler.removeCallbacks(dotsRunnable);
                dotsRunnable = null; // clearing the runnable
            }

        }

    }

    // -------------------- LIFECYCLE SAFETY ---------------------

    /**
     * This function ensures the animation stops when a view is recycled.
     * @param holder The ViewHolder for the view being recycled.
     */
    @Override
    public final void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {

        // PROCESS: stopping the animation
        MsgVH vh = (MsgVH) holder;
        vh.stopPulse();
        vh.stopDots();

        super.onViewRecycled(holder); // calling the parent method

    }

    /**
     * This function ensures the animation stops when a view is detached.
     * @param holder The ViewHolder of the view being detached.
     */
    @Override
    public final void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {

        // PROCESS: stopping the animation
        MsgVH vh = (MsgVH) holder;
        vh.stopPulse();

        super.onViewDetachedFromWindow(holder); // calling the parent method

    }

}
