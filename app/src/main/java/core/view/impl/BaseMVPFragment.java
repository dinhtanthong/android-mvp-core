package core.view.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;

import core.presenter.Presenter;
import core.presenter.loader.PresenterFactory;
import core.presenter.loader.PresenterLoader;
import core.view.BaseView;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public abstract class BaseMVPFragment<P extends Presenter<V>, V extends BaseView> extends Fragment implements LoaderManager.LoaderCallbacks<P> {
    private final static String TAG = BaseMVPFragment.class.getName();
    private final static int LOADER_ID = 10011990;

    /**
     * The presenter for this view
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    protected P mPresenter;
    /**
     * Is this the first start of the fragment (after onCreate)
     */
    private boolean mFirstStart;
    /**
     * Do we need to call {@link #doStart()} from the {@link #onLoadFinished(Loader, P)} method.
     * Will be true if presenter wasn't loaded when {@link #onStart()} is reached
     */
    @VisibleForTesting
    final AtomicBoolean mNeedToCallStart = new AtomicBoolean(false);

// ------------------------------------------->

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirstStart = true;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // When used in backstack, the fragment will not be destroyed, only its view.
        mFirstStart = true;

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mPresenter == null) {
            mNeedToCallStart.set(true);
            Log.d(TAG, "Start postponed, presenter not ready");
        } else {
            doStart();
        }
    }

    @Override
    public void onStop() {
        if (mPresenter != null) {
            doStop();
        }

        super.onStop();
    }

// ------------------------------------------->

    /**
     * Call the presenter callbacks for onStart
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    void doStart() {
        assert mPresenter != null;

        mPresenter.onViewAttached((V) this);

        mPresenter.onStart(mFirstStart);

        mFirstStart = false;
    }

    /**
     * Call the presenter callbacks for onStop
     */
    @VisibleForTesting
    void doStop() {
        assert mPresenter != null;

        mPresenter.onStop();

        mPresenter.onViewDetached();
    }

    @Override
    public final Loader<P> onCreateLoader(int id, Bundle args) {
        return new PresenterLoader<>(getContext(), getPresenterFactory());
    }

    @Override
    public final void onLoadFinished(Loader<P> loader, P presenter) {
        mPresenter = presenter;

        if (mNeedToCallStart.compareAndSet(true, false)) {
            doStart();
            Log.d(TAG, "Postponed start called");
        }
    }

    @Override
    public final void onLoaderReset(Loader<P> loader) {
        mPresenter = null;
    }

    /**
     * Get the presenter factory implementation for this view
     *
     * @return the presenter factory
     */
    protected abstract PresenterFactory<P> getPresenterFactory();
}
