package br.edu.uemg.cannongame;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Fragmento que contém o CannonView e controla o ciclo de vida do jogo. */
public class CannonGameFragment extends Fragment {

    private CannonView cannonView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);
        cannonView = view.findViewById(R.id.cannonView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Os botões de volume controlam o volume do jogo
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cannonView != null) {
            cannonView.stopGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cannonView != null) {
            cannonView.releaseResources();
        }
    }
}
