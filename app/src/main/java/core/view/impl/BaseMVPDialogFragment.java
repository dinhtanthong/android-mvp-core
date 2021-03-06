/*
 *   Copyright 2017 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package core.view.impl;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.DialogFragment;
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
public abstract class BaseMVPDialogFragment<P extends Presenter<V>, V extends BaseView> extends DialogFragment implements LoaderManager.LoaderCallbacks<P>
{
    private final static String TAG = BaseMVPFragment.class.getName();
    private final static int LOADER_ID = 17051992;

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
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mFirstStart = true;

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        mFirstStart = true;

        super.onViewCreated(view, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        mFirstStart = true;

        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if( mPresenter == null )
        {
            mNeedToCallStart.set(true);
            Log.d(TAG, "Start postponed, presenter not ready");
        }
        else
        {
            doStart();
        }
    }

    @Override
    public void onStop()
    {
        if( mPresenter != null )
        {
            doStop();
        }

        super.onStop();
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        // Force destroy manually cause it's not called by the framework, not sure why yet
        if( getActivity() != null ) // If get activity == null, it's just a rotation
        {
            getLoaderManager().destroyLoader(LOADER_ID);
        }

        super.onDismiss(dialog);
    }

// ------------------------------------------->

    /**
     * Call the presenter callbacks for onStart
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    void doStart()
    {
        assert mPresenter != null;

        mPresenter.onViewAttached((V) this);

        mPresenter.onStart(mFirstStart);

        mFirstStart = false;
    }

    /**
     * Call the presenter callbacks for onStop
     */
    @VisibleForTesting
    void doStop()
    {
        assert mPresenter != null;

        mPresenter.onStop();

        mPresenter.onViewDetached();
    }

    @Override
    public final Loader<P> onCreateLoader(int id, Bundle args)
    {
        return new PresenterLoader<>(getContext(), getPresenterFactory());
    }

    @Override
    public final void onLoadFinished(Loader<P> loader, P presenter)
    {
        mPresenter = presenter;

        if( mNeedToCallStart.compareAndSet(true, false) )
        {
            doStart();
            Log.d(TAG, "Postponed start called");
        }
    }

    @Override
    public final void onLoaderReset(Loader<P> loader)
    {
        mPresenter = null;
    }

    /**
     * Get the presenter factory implementation for this view
     *
     * @return the presenter factory
     */
    protected abstract PresenterFactory<P> getPresenterFactory();
}
