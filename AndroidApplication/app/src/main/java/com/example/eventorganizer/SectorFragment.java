package com.example.eventorganizer;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SectorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SectorFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mTitle;

    public SectorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SectorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SectorFragment newInstance(String title) {
        SectorFragment fragment = new SectorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View sectorField = Objects.requireNonNull(getView()).findViewById(R.id.sector1_field);
        TextView sectorName = Objects.requireNonNull(getView()).findViewById(R.id.sector1_name);
        sectorField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HomeActivity) Objects.requireNonNull(getActivity())).setRoomActivity(sectorName.getText().toString());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((HomeActivity) Objects.requireNonNull(getActivity())).rooms.setVisible(false);
        Objects.requireNonNull(getActivity()).setTitle(mTitle);
        ((HomeActivity)getActivity()).navigationView.setCheckedItem(R.id.nav_sectors);
        ((HomeActivity) getActivity()).setSelectedItemId(R.id.nav_sectors);
        return inflater.inflate(R.layout.fragment_sector, container, false);
    }
}