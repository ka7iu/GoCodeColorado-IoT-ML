package com.paultrebilcoxruiz.wildlifemap;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.TextView;

public class MapBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private MapLayerSelectionListener mListener;

    public MapBottomSheetDialogFragment(MapLayerSelectionListener listener) {
        mListener = listener;
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_fragment, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if( behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        contentView.findViewById(R.id.wildlife_bear_human_conflict).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_mountain_lion_human_conflict).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_black_bear_summer).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_black_bear_fall).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_black_bear_all).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_canadian_goose_winter).setOnClickListener(this);
        contentView.findViewById(R.id.wilfelife_elk_summer).setOnClickListener(this);
        contentView.findViewById(R.id.wilfelife_elk_winter).setOnClickListener(this);
        contentView.findViewById(R.id.wilfelife_elk_severe_winter).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_mountain_goat_summer).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_mountain_goat_winter).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_mountain_lion_all).setOnClickListener(this);
        contentView.findViewById(R.id.wildlife_moose_all).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        onTextViewClicked(v);
    }

    public interface MapLayerSelectionListener {
        void onMapLayerSelected(String layer);
    }

    public void onTextViewClicked(View v) {
        mListener.onMapLayerSelected(((TextView) v).getText().toString());
        dismiss();
    }

}
