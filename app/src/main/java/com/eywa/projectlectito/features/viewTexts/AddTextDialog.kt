package com.eywa.projectlectito.features.viewTexts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.eywa.projectlectito.R
import com.eywa.projectlectito.utils.ToastSpamPrevention

class AddTextDialog : DialogFragment() {
    private val viewModel: ViewTextsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(requireActivity().resources.getString(R.string.add_texts__title))
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.add_text_dialog, null)
        builder.setView(view)
        builder.setPositiveButton(requireActivity().resources.getString(R.string.add_texts__ok_button)) { _, _ ->
            val textName = view.findViewById<EditText>(R.id.input_text_atd__name).text.toString()

            if (textName.isBlank()) {
                ToastSpamPrevention.displayToast(
                        requireContext(),
                        resources.getString(R.string.err_add_texts__name_blank)
                )
                return@setPositiveButton
            }

            viewModel.insert(textName)
        }
        builder.setNegativeButton(requireActivity().resources.getString(R.string.cancel)) { _, _ -> }
        return builder.create()
    }
}