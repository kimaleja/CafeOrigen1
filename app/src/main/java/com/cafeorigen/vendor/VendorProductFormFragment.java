package com.cafeorigen.vendor;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.core.util.Validators;
import com.cafeorigen.databinding.FragmentVendorProductFormBinding;

/**
 * CafeOrigen — Formulario de producto del Vendedor (V3 crear / V4 editar).
 * Imagen desde galería (GetContent; cámara solo placeholder), categoría,
 * validaciones locales y dos salidas: guardar borrador o enviar a moderación.
 */
public class VendorProductFormFragment extends Fragment {

    private static final String ARG_PRODUCT_ID = "productId";
    private static final int COFFEE_MIN_ALTITUDE = 800;
    private static final int COFFEE_MAX_ALTITUDE = 2600;
    private static final int PUBLISH_MIN_DESCRIPTION = 20;

    private FragmentVendorProductFormBinding binding;
    private VendorProductsViewModel viewModel;
    private Product editing;        // null = crear (V3)
    private ProductCategory selectedCategory;
    private String selectedImageUri; // uri de galería (V3)
    private ActivityResultLauncher<String> galleryPicker;

    /** Fábrica para creación (V3). */
    public static VendorProductFormFragment newCreate() {
        return new VendorProductFormFragment();
    }

    /** Fábrica para edición (V4). */
    public static VendorProductFormFragment newEdit(String productId) {
        VendorProductFormFragment fragment = new VendorProductFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorProductFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(VendorProductsViewModel.class);

        // Selección de imagen: galería sin permisos especiales (V3).
        galleryPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        selectedImageUri = uri.toString();
                        binding.productFormImage.setImageURI(uri);
                    }
                });

        if (getArguments() != null) {
            String productId = getArguments().getString(ARG_PRODUCT_ID, null);
            if (productId != null) {
                editing = findOwnProduct(productId);
            }
        }

        boolean isEdit = editing != null;
        binding.productFormTitleText.setText(getString(isEdit
                ? R.string.vendor_edit_product_title : R.string.vendor_new_product_title));

        setupCategoryDropdown();
        setupActions(isEdit);
        observeOps();

        if (isEdit) {
            fillEditMode();
        }
    }

    /** El producto debe ser del vendedor en sesión (permisos por vendorId). */
    private Product findOwnProduct(String productId) {
        String vendorId = SessionManager.get(requireContext()).getUserId();
        Product own = viewModel.findOwn(productId);
        if (own != null && own.vendorId.equals(vendorId)) {
            return own;
        }
        own = MockDataProvider.findProductById(productId);
        return own != null && own.vendorId.equals(vendorId) ? own : null;
    }

    private void setupCategoryDropdown() {
        ProductCategory[] categories = ProductCategory.choices();
        String[] labels = new String[categories.length];
        for (int index = 0; index < categories.length; index++) {
            labels[index] = categories[index].getLabel();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, labels);
        binding.productFormCategoryEdit.setAdapter(adapter);
        binding.productFormCategoryEdit.setOnItemClickListener((parent, v, position, id) ->
                selectedCategory = categories[position]);
    }

    private void setupActions(boolean isEdit) {
        binding.productFormBack.setOnClickListener(v -> popBack());

        binding.productFormGalleryButton.setOnClickListener(v -> galleryPicker.launch("image/*"));
        // Cámara: placeholder deshabilitado (se habilitará al definir permisos).
        binding.productFormCameraButton.setOnClickListener(null);

        binding.productFormDraftButton.setOnClickListener(v -> submit(true, isEdit));
        binding.productFormSubmitButton.setOnClickListener(v -> submit(false, isEdit));

        if (isEdit && editing.status == ProductStatus.ACTIVO) {
            // Ya publicado: reenviar no pasa por moderación.
            binding.productFormSubmitButton.setText(R.string.vendor_save_changes);
            binding.productFormDraftButton.setText(R.string.vendor_save_changes);
        }
    }

    private void observeOps() {
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case CREATED:
                case UPDATED:
                    Toast.makeText(requireContext(), getString(
                            state.status == VendorProductsViewModel.OpsStatus.CREATED
                                    ? R.string.vendor_created_fmt : R.string.vendor_updated_fmt,
                            String.valueOf(state.productName)), Toast.LENGTH_SHORT).show();
                    popBack();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.vendor_ops_error,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });
    }

    private void submit(boolean asDraft, boolean isEdit) {
        if (!validateFields(asDraft)) {
            return;
        }
        String vendorId = SessionManager.get(requireContext()).getUserId();
        String vendorName = SessionManager.get(requireContext()).getUserName();

        ProductStatus status;
        if (asDraft) {
            status = ProductStatus.BORRADOR;
        } else if (isEdit && editing != null && editing.status == ProductStatus.ACTIVO) {
            status = ProductStatus.ACTIVO; // re-edición de café ya publicado
        } else {
            status = ProductStatus.PENDIENTE; // enviar a moderación
        }

        long price = parseLong(binding.productFormPriceEdit.getText());
        // El proceso no se pide en el formulario; el mock lo tolera vacío.
        VendorProductsViewModel.Draft draft = new VendorProductsViewModel.Draft(
                isEdit ? editing.id : null,
                String.valueOf(binding.productFormNameEdit.getText()).trim(),
                String.valueOf(binding.productFormRegionEdit.getText()).trim(),
                String.valueOf(binding.productFormVarietyEdit.getText()).trim(),
                "",
                parseAltitudeSafe(),
                price,
                parseInt(binding.productFormStockEdit.getText()),
                String.valueOf(binding.productFormDescriptionEdit.getText()).trim(),
                selectedImageUri,
                selectedCategory,
                status);

        viewModel.save(draft, vendorId, vendorName);
    }

    private int parseAltitudeSafe() {
        CharSequence text = binding.productFormAltitudeEdit.getText();
        if (text == null || text.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(text.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(CharSequence text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        try {
            return Long.parseLong(text.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseInt(CharSequence text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(text.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Validaciones por campo; asDraft relaja la descripción mínima. */
    private boolean validateFields(boolean asDraft) {
        boolean valid = true;
        clearErrors();

        String name = String.valueOf(binding.productFormNameEdit.getText()).trim();
        if (!Validators.notEmpty(name)) {
            binding.productFormNameInput.setError(getString(R.string.vendor_error_name));
            valid = false;
        }
        if (selectedCategory == null) {
            binding.productFormCategoryInput.setError(getString(R.string.vendor_error_category));
            valid = false;
        }
        long price = parseLong(binding.productFormPriceEdit.getText());
        if (price <= 0) {
            binding.productFormPriceInput.setError(getString(R.string.vendor_error_price));
            valid = false;
        }
        long stock = parseLong(binding.productFormStockEdit.getText());
        if (stock < 0) {
            binding.productFormStockInput.setError(getString(R.string.vendor_error_stock));
            valid = false;
        }

        boolean isCoffee = selectedCategory != null
                && selectedCategory != ProductCategory.ACCESORIOS;
        if (isCoffee) {
            String region = String.valueOf(binding.productFormRegionEdit.getText()).trim();
            if (!Validators.notEmpty(region)) {
                binding.productFormRegionInput.setError(getString(R.string.vendor_error_region));
                valid = false;
            }
            int altitude = parseAltitudeSafe();
            if (altitude < COFFEE_MIN_ALTITUDE || altitude > COFFEE_MAX_ALTITUDE) {
                binding.productFormAltitudeInput.setError(
                        getString(R.string.vendor_error_altitude_fmt,
                                COFFEE_MIN_ALTITUDE, COFFEE_MAX_ALTITUDE));
                valid = false;
            }
        }

        String description = String.valueOf(binding.productFormDescriptionEdit.getText()).trim();
        if (!asDraft && description.length() < PUBLISH_MIN_DESCRIPTION) {
            binding.productFormDescriptionInput.setError(
                    getString(R.string.vendor_error_description));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        binding.productFormNameInput.setError(null);
        binding.productFormCategoryInput.setError(null);
        binding.productFormPriceInput.setError(null);
        binding.productFormStockInput.setError(null);
        binding.productFormRegionInput.setError(null);
        binding.productFormAltitudeInput.setError(null);
        binding.productFormDescriptionInput.setError(null);
    }

    private void fillEditMode() {
        binding.productFormNameEdit.setText(editing.name);
        binding.productFormPriceEdit.setText(String.valueOf(editing.price));
        binding.productFormStockEdit.setText(String.valueOf(editing.stock));
        binding.productFormDescriptionEdit.setText(editing.description);
        binding.productFormRegionEdit.setText(editing.region == null ? "" : editing.region);
        binding.productFormVarietyEdit.setText(editing.variety == null ? "" : editing.variety);
        if (editing.altitudeM > 0) {
            binding.productFormAltitudeEdit.setText(String.valueOf(editing.altitudeM));
        }
        if (editing.category != null) {
            selectedCategory = editing.category;
            binding.productFormCategoryEdit.setText(editing.category.getLabel(), false);
        }
        if (editing.imageUri != null && !editing.imageUri.isEmpty()) {
            selectedImageUri = editing.imageUri;
            binding.productFormImage.setImageURI(Uri.parse(editing.imageUri));
        }
    }

    private void popBack() {
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
