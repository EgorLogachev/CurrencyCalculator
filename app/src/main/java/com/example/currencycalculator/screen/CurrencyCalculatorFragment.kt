package com.example.currencycalculator.screen

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currencycalculator.data.Conversion
import com.example.currencycalculator.data.Currency
import com.example.currencycalculator.data.utils.getFormattedDate
import com.example.currencycalculator.databinding.FragmentFirstBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.schedule

@AndroidEntryPoint
class CurrencyCalculatorFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var selectedDate = Date()
    private val viewModel: CurrencyViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val originAdapter = ChooserAdapter()
    private val targetAdapter = ChooserAdapter()
    private val historyAdapter = HistoryAdapter()

    private var originalCurrency: Currency? = null
    private var targetCurrency: Currency? = null
    private val historyDebouncer = Debouncer(2000L) {
        val originalAmount = originalAmount()
        val targetAmount = targetAmount()
        if (originalCurrency != null
            && targetCurrency != null
            && originalAmount != 0f
            && targetAmount != 0f
        ) {
            viewModel.updateHistory(
                Conversion(
                    originalAmount = originalAmount,
                    targetAmount = targetAmount,
                    originalCurrency = originalCurrency!!,
                    targetCurrency = targetCurrency!!
                )
            )
        }
    }

    private val originalTextWatcher = CurrencyTextWatcher {
        val targetAmount =
            convertCurrencies(it.toFloat(), originalCurrency, targetCurrency)
        binding.targetCurrencyTextField.setText(targetAmount.toFormattedString("%.2f"))
        historyDebouncer.debounce()
    }

    private val targetTextWatcher = CurrencyTextWatcher {
        val originalAmount =
            convertCurrencies(it.toFloat(), targetCurrency, originalCurrency)
        binding.originalCurrencyTextField.setText(originalAmount.toFormattedString("%.2f"))
        historyDebouncer.debounce()
    }

    private fun convertCurrencies(
        originalAmount: Float,
        originalCurrency: Currency?,
        targetCurrency: Currency?
    ) = originalAmount * (originalCurrency?.rate ?: 0f) / (targetCurrency?.rate ?: 1f)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // fetch data from ViewModel
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.catch {
                    Toast.makeText(
                        requireContext(),
                        it.message ?: "UNEXPECTED ERROR!!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }.collect {
                    if (it.isLoading) {
                        setUiEnabled(false)
                    } else {
                        setUiEnabled(true)
                        originAdapter.setCurrencies(it.currencies)
                        targetAdapter.setCurrencies(it.currencies)
                        historyAdapter.setHistory(it.history)
                    }
                }
            }
        }

        // setup UI
        binding.apply {
            // date chooser setup
            dateChooser.text = Calendar.getInstance().getFormattedDate()
            dateChooser.setOnClickListener {
                DatePickerFragment.newInstance(selectedDate).apply {
                    setOnDatePickedListener {
                        selectedDate = it.apply {
                            binding.dateChooser.text = getFormattedDate()
                            viewModel.fetchCurrencies(this)
                        }
                        historyDebouncer.debounce()
                    }
                }.show(parentFragmentManager, DatePickerFragment.DATE_PICKER_TAG)
            }

            //original text field setup
            originalCurrencyTextField.filters = arrayOf(DecimalDigitsInputFilter())
            originalCurrencyTextField.setOnFocusChangeListener(::onTextFieldFocusChanged)

            // target text field setup
            targetCurrencyTextField.filters = arrayOf(DecimalDigitsInputFilter())
            targetCurrencyTextField.setOnFocusChangeListener(::onTextFieldFocusChanged)

            //original currency chooser setup
            originalCurrencyChooser.adapter = originAdapter
            originalCurrencyChooser.onCurrencySelected {
                originalCurrency = it
                val targetAmount = convertCurrencies(
                    originalAmount(),
                    it,
                    targetCurrency
                )
                binding.targetCurrencyTextField.setText(targetAmount.toFormattedString("%.2f"))
                historyDebouncer.debounce()
            }

            //target currency chooser setup
            targetCurrencyChooser.adapter = targetAdapter
            targetCurrencyChooser.onCurrencySelected {
                targetCurrency = it
                val targetAmount = convertCurrencies(
                    originalAmount(),
                    originalCurrency,
                    it
                )
                binding.targetCurrencyTextField.setText(targetAmount.toFormattedString("%.2f"))
                historyDebouncer.debounce()
            }

            operationsHistory.layoutManager = LinearLayoutManager(requireContext())
            operationsHistory.adapter = historyAdapter
        }
    }

    private fun originalAmount() = binding.originalCurrencyTextField.text.toFloat()

    private fun targetAmount() = binding.targetCurrencyTextField.text.toFloat()

    private fun onTextFieldFocusChanged(view: View, hasFocus: Boolean) {
        binding.apply {
            if (view == originalCurrencyTextField) {
                val func = if (hasFocus) originalCurrencyTextField::addTextChangedListener
                else originalCurrencyTextField::removeTextChangedListener
                func.invoke(originalTextWatcher)
            } else if (view == targetCurrencyTextField) {
                val func = if (hasFocus) targetCurrencyTextField::addTextChangedListener
                else targetCurrencyTextField::removeTextChangedListener
                func.invoke(targetTextWatcher)
            }
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.apply {
            dateChooser.isEnabled = enabled
            originalCurrencyTextField.isEnabled = enabled
            targetCurrencyTextField.isEnabled = enabled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.originalCurrencyTextField.removeTextChangedListener(originalTextWatcher)
        binding.targetCurrencyTextField.removeTextChangedListener(targetTextWatcher)
        _binding = null
    }

    class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

        companion object {
            const val DATE_PICKER_TAG = "date_picker"
            private const val DATE_PARAM = "date"

            fun newInstance(initialDate: Date): DatePickerFragment {
                return DatePickerFragment().apply {
                    arguments = Bundle().apply {
                        putLong(DATE_PARAM, initialDate.time)
                    }
                }
            }
        }

        private var onDatePickedListener: ((Date) -> Unit)? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val c = Calendar.getInstance().apply {
                val args = savedInstanceState ?: arguments
                time = Date(args!!.getLong(DATE_PARAM))
            }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of TimePickerDialog and return it
            return DatePickerDialog(requireActivity(), this, year, month, day).apply {
                datePicker.maxDate = Date().time
            }
        }

        override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
            onDatePickedListener?.invoke(Calendar.getInstance().apply {
                set(p1, p2, p3)
            }.time)
        }

        fun setOnDatePickedListener(onPicked: (Date) -> Unit) {
            onDatePickedListener = onPicked
        }
    }
}

fun Editable?.toFloat(): Float {
    if (isNullOrBlank()) return 0f
    return toString().toFloat()
}

fun Float.toFormattedString(format: String) = String.format(format, this)

inline fun Spinner.onCurrencySelected(crossinline onCurrencySelected: (Currency) -> Unit) {
    this.onItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            onCurrencySelected.invoke(adapter.getItem(p2) as Currency)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}
    }
}

internal class ChooserAdapter(private var currencies: List<Currency> = emptyList()) :
    BaseAdapter() {

    override fun getCount() = currencies.size

    override fun getItem(p0: Int) = currencies[p0]

    override fun getItemId(p0: Int): Long = p0.toLong()

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val item = p1 ?: LayoutInflater
            .from(p2?.context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, p2, false)
        return item.findViewById<TextView>(android.R.id.text1).apply {
            text = currencies[p0].text
        }
    }

    fun setCurrencies(currencies: List<Currency>) {
        this.currencies = currencies
        notifyDataSetChanged()
    }
}

internal class DecimalDigitsInputFilter : InputFilter {

    private val pattern =
        Pattern.compile("\\d+((\\.\\d?)?)|(\\.)?")

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val matcher: Matcher = pattern.matcher(dest)
        return if (!matcher.matches()) "" else null
    }
}

internal class CurrencyTextWatcher(private val afterTextChanged: (editable: Editable?) -> Unit) :
    TextWatcher {

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        afterTextChanged.invoke(p0)
    }
}

internal class Debouncer(private val delay: Long, private val action: () -> Unit) {

    private var timer = Timer()

    fun debounce() {
        timer.cancel()

        timer = Timer()
        timer.schedule(delay) {
            action.invoke()
        }
    }
}

internal class HistoryAdapter(private var history: List<Conversion> = emptyList()) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textView: TextView

        init {
            textView = view.findViewById(android.R.id.text1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    fun setHistory(history: List<Conversion>) {
        this.history = history
        notifyDataSetChanged()
    }

    override fun getItemCount() = history.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversion = history[position]
        holder.textView.text =
            "${conversion.originalAmount} ${conversion.originalCurrency.text} -> ${conversion.targetAmount} ${conversion.targetCurrency.text}"
    }
}
