package dev.forcetower.hilt.android.dynamic.components;

import androidx.fragment.app.Fragment;
import dagger.hilt.internal.GeneratedComponent;

public interface DynamicFragmentComponent extends GeneratedComponent {
    DynamicViewModelComponent.Builder viewModelComponentBuilder();
    interface Builder {
        Builder fragment(Fragment fragment);
        DynamicFragmentComponent build();
    }
}
