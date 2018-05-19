package com.gianlu.pretendyourexyzzy.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class EditGameOptionsDialog extends DialogFragment {
    private TextInputLayout scoreLimit;
    private TextInputLayout playerLimit;
    private TextInputLayout spectatorLimit;
    private Spinner timerMultiplier;
    private TextInputLayout blankCards;
    private TextInputLayout password;
    private LinearLayout cardSets;
    private LinearLayout layout;
    private int gid;
    private ApplyOptions listener;

    @NonNull
    public static EditGameOptionsDialog get(int gid, Game.Options options) {
        EditGameOptionsDialog dialog = new EditGameOptionsDialog();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        args.putSerializable("options", options);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ApplyOptions)
            listener = (ApplyOptions) context;
    }

    private void done() {
        scoreLimit.setErrorEnabled(false);
        playerLimit.setErrorEnabled(false);
        spectatorLimit.setErrorEnabled(false);
        blankCards.setErrorEnabled(false);
        password.setErrorEnabled(false);

        Game.Options newOptions;
        try {
            newOptions = Game.Options.validateAndCreate(timerMultiplier.getSelectedItem().toString(), CommonUtils.getText(spectatorLimit), CommonUtils.getText(playerLimit), CommonUtils.getText(scoreLimit), CommonUtils.getText(blankCards), cardSets, CommonUtils.getText(password));
        } catch (Game.Options.InvalidFieldException ex) {
            View view = layout.findViewById(ex.fieldId);
            if (view != null && view instanceof TextInputLayout) {
                if (ex.throwMessage == R.string.outOfRange)
                    ((TextInputLayout) view).setError(getString(R.string.outOfRange, ex.min, ex.max));
                else
                    ((TextInputLayout) view).setError(getString(ex.throwMessage));
            }

            return;
        }

        dismiss();
        if (listener != null) listener.changeGameOptions(gid, newOptions);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.TitledDialog);
        dialog.setTitle(R.string.editGameOptions);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.dialog_edit_game_options, container, false);

        Bundle args = getArguments();
        Game.Options options;
        if (args == null || (options = (Game.Options) args.getSerializable("options")) == null
                || (gid = args.getInt("gid", -1)) == -1 || getContext() == null) {
            dismiss();
            return layout;
        }

        RegisteredPyx pyx;
        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
            dismiss();
            return layout;
        }

        scoreLimit = layout.findViewById(R.id.editGameOptions_scoreLimit);
        CommonUtils.setText(scoreLimit, String.valueOf(options.scoreLimit));

        playerLimit = layout.findViewById(R.id.editGameOptions_playerLimit);
        CommonUtils.setText(playerLimit, String.valueOf(options.playersLimit));

        spectatorLimit = layout.findViewById(R.id.editGameOptions_spectatorLimit);
        CommonUtils.setText(spectatorLimit, String.valueOf(options.spectatorsLimit));

        timerMultiplier = layout.findViewById(R.id.editGameOptions_timerMultiplier);
        timerMultiplier.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Game.Options.VALID_TIMER_MULTIPLIERS));
        timerMultiplier.setSelection(Game.Options.timerMultiplierIndex(options.timerMultiplier));

        blankCards = layout.findViewById(R.id.editGameOptions_blankCards);
        CommonUtils.setText(blankCards, String.valueOf(options.blanksLimit));

        password = layout.findViewById(R.id.editGameOptions_password);
        CommonUtils.setText(password, options.password);

        cardSets = layout.findViewById(R.id.editGameOptions_cardSets);
        cardSets.removeAllViews();
        for (CardSet set : pyx.firstLoad().cardSets) {
            CheckBox item = new CheckBox(getContext());
            item.setTag(set);
            item.setText(set.name);
            item.setChecked(options.cardSets.contains(set.id));
            cardSets.addView(item);
        }

        Button cancel = layout.findViewById(R.id.editGameOptions_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Button apply = layout.findViewById(R.id.editGameOptions_apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });

        return layout;
    }

    public interface ApplyOptions {
        void changeGameOptions(int gid, @NonNull Game.Options options);
    }
}