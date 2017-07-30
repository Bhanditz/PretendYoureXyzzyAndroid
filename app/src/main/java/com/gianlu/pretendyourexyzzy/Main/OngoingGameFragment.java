package com.gianlu.pretendyourexyzzy.Main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.GameManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class OngoingGameFragment extends Fragment implements PYX.IResult<GameInfo>, GameManager.IManager {
    private IFragment handler;
    private Game game;
    private FrameLayout layout;
    private ProgressBar loading;
    private LinearLayout container;
    private GameManager manager;
    private User me;
    private PYX pyx;

    public static OngoingGameFragment getInstance(Game game, User me, OngoingGameFragment.IFragment handler) {
        OngoingGameFragment fragment = new OngoingGameFragment();
        fragment.handler = handler;
        fragment.setHasOptionsMenu(true);
        Bundle args = new Bundle();
        args.putSerializable("me", me);
        args.putSerializable("game", game);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.ongoing_game_fragment, parent, false);
        loading = (ProgressBar) layout.findViewById(R.id.ongoingGame_loading);
        container = (LinearLayout) layout.findViewById(R.id.ongoingGame_container);

        me = (User) getArguments().getSerializable("me");
        game = (Game) getArguments().getSerializable("game");
        if (game == null) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        pyx = PYX.get(getContext());
        pyx.getGameInfo(game.gid, this);

        return layout;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) updateActivityTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityTitle();
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (manager != null && activity != null && isVisible())
            activity.setTitle(manager.gameInfo.game.host + " - " + getString(R.string.app_name));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ongoing_game, menu);
    }

    private void leaveGame() {
        if (game == null) return;
        PYX.get(getContext()).leaveGame(game.gid, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                if (handler != null) handler.onLeftGame();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_LEAVING, ex);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ongoingGame_leave:
                leaveGame();
                return true;
            case R.id.ongoingGame_options:
                showGameOptions();
                return true;
            case R.id.ongoingGame_spectators:
                showSpectators();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSpectators() {
        if (game == null) return;
        SuperTextView spectators = new SuperTextView(getContext(), R.string.spectatorsList, game.spectators.isEmpty() ? "none" : CommonUtils.join(game.spectators, ", "));
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        spectators.setPadding(padding, padding, padding, padding);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.spectatorsLabel)
                .setView(spectators)
                .setPositiveButton(android.R.string.ok, null);

        CommonUtils.showDialog(getActivity(), builder);
    }

    @SuppressLint("InflateParams")
    private void showGameOptions() {
        if (game == null) return;
        Game.Options options = game.options;
        ScrollView layout = (ScrollView) LayoutInflater.from(getContext()).inflate(R.layout.game_options_dialog, null, false);
        SuperTextView scoreLimit = (SuperTextView) layout.findViewById(R.id.gameOptions_scoreLimit);
        scoreLimit.setHtml(R.string.scoreLimit, options.scoreLimit);
        SuperTextView playerLimit = (SuperTextView) layout.findViewById(R.id.gameOptions_playerLimit);
        playerLimit.setHtml(R.string.playerLimit, options.playersLimit);
        SuperTextView spectatorLimit = (SuperTextView) layout.findViewById(R.id.gameOptions_spectatorLimit);
        spectatorLimit.setHtml(R.string.spectatorLimit, options.spectatorsLimit);
        SuperTextView idleTimeMultiplier = (SuperTextView) layout.findViewById(R.id.gameOptions_idleTimeMultiplier);
        idleTimeMultiplier.setHtml(R.string.timeMultiplier, options.timeMultiplier);
        SuperTextView cardSets = (SuperTextView) layout.findViewById(R.id.gameOptions_cardSets);
        cardSets.setHtml(R.string.cardSets, CommonUtils.join(pyx.firstLoad.createCardSetNamesList(options.cardSets), ", "));

        SuperTextView blankCards = (SuperTextView) layout.findViewById(R.id.gameOptions_blankCards);
        blankCards.setHtml(R.string.blankCards, options.blanksLimit);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.gameOptions)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null);

        CommonUtils.showDialog(getActivity(), builder);
    }

    @Override
    public void onDone(PYX pyx, final GameInfo gameInfo) {
        if (manager == null) manager = new GameManager(container, gameInfo, me, this);
        pyx.pollingThread.addListener(manager);
        updateActivityTitle();

        pyx.getGameCards(gameInfo.game.gid, new PYX.IResult<GameCards>() {
            @Override
            public void onDone(PYX pyx, GameCards gameCards) {
                manager.setCards(gameCards);
                loading.setVisibility(View.GONE);
                container.setVisibility(View.VISIBLE);
                MessageLayout.hide(layout);
            }

            @Override
            public void onException(Exception ex) {
                OngoingGameFragment.this.onException(ex);
            }
        });
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void notifyWinner(String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.winnerIs, nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void notifyPlayerSkipped(String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.playerSkipped, nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void notifyJudgeSkipped(String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.judgeSkipped, nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void hurryUp() {
        Toaster.show(getActivity(), Utils.Messages.HURRY_UP);
    }

    public interface IFragment {
        void onLeftGame();
    }
}