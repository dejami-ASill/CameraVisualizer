package com.devsil.cameravisualizer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devsil.cameravisualizer.Camera.CameraSurfaceRenderer;

/**
 * Created by devsil on 11/4/2017.
 */

public class OptionsFragment extends Fragment {


    private LinearLayout llRootView;

    private TextView mRectEffectBtn;
    private TextView mTriangleEffectBtn;
    private TextView mNoEffectBtn;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        llRootView =(LinearLayout) inflater.inflate(R.layout.options_fragment, container, false);

        mRectEffectBtn = (TextView)llRootView.findViewById(R.id.effect_rect);
        mTriangleEffectBtn = (TextView)llRootView.findViewById(R.id.effect_triangle);
        mNoEffectBtn = (TextView)llRootView.findViewById(R.id.effect_none);

        mRectEffectBtn.setOnClickListener(EFFECT_CLICK);
        mTriangleEffectBtn.setOnClickListener(EFFECT_CLICK);
        mNoEffectBtn.setOnClickListener(EFFECT_CLICK);


        return llRootView;
    }


    private View.OnClickListener EFFECT_CLICK = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.effect_rect:
                    ((CameraActivity)getActivity()).onShapeSelected(CameraSurfaceRenderer.MODE.RECT);
                    break;
                case R.id.effect_none:
                       ((CameraActivity)getActivity()).onShapeSelected(CameraSurfaceRenderer.MODE.NONE);
                    break;
                case R.id.effect_triangle:
                    ((CameraActivity)getActivity()).onShapeSelected(CameraSurfaceRenderer.MODE.TRIANGLE);
                    break;
            }
        }
    };

    public interface OptionsMenuListener{
        void onShapeSelected(CameraSurfaceRenderer.MODE mode);
    }
}
