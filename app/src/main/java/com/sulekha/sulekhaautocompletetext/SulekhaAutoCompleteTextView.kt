package com.sulekha.sulekhaautocompletetext

import android.content.Context
import android.content.res.Resources
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.QwertyKeyListener
import android.util.AttributeSet
import android.widget.AutoCompleteTextView

/**
 * An editable text view, extending [AutoCompleteTextView], that
 * can show completion suggestions for the substring of the text where
 * the user is typing instead of necessarily for the entire thing.
 *
 *
 * You must provide a [SulekhaAutoCompleteTextView.Tokenizer] to distinguish the
 * various substrings.
 *
 *
 * The following code snippet shows how to create a text view which suggests
 * various countries names while the user is typing:
 *
 * <pre class="prettyprint">
 * public class CountriesActivity extends Activity {
 * protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * setContentView(R.layout.autocomplete_7);
 *
 * ArrayAdapter&lt;String&gt; adapter = new ArrayAdapter&lt;String&gt;(this,
 * android.R.layout.simple_dropdown_item_1line, COUNTRIES);
 * SulekhaAutoCompleteTextView textView = findViewById(R.id.edit);
 * textView.setAdapter(adapter);
 * textView.setTokenizer(new SulekhaAutoCompleteTextView.SulekhaTokenizer());
 * }
 *
 * private static final String[] COUNTRIES = new String[] {
 * "Belgium", "France", "Italy", "Germany", "Spain"
 * };
 * }</pre>
 */


class SulekhaAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = Resources.getSystem().getIdentifier("autoCompleteTextViewStyle", "attr", "android"),
    defStyleRes: Int = 0
) : android.support.v7.widget.AppCompatAutoCompleteTextView(
    context, attrs,
    Resources.getSystem().getIdentifier("autoCompleteTextViewStyle", "attr", "android")
) {
    private var mTokenizer: Tokenizer? = null
    private var mIsRemoveToken = false

    /* package */ internal fun finishInit() {}

    /**
     * Sets the Tokenizer that will be used to determine the relevant
     * range of the text where the user is typing.
     */
    fun setTokenizer(t: Tokenizer) {
        mTokenizer = t
    }

    /**
     * Instead of filtering on the entire contents of the edit box,
     * this subclass method filters on the range from
     * [Tokenizer.findTokenStart] to [.getSelectionEnd]
     * if the length of that range meets or exceeds [.getThreshold].
     */
    override fun performFiltering(text: CharSequence, keyCode: Int) {
        if (enoughToFilter()) {
            val end = selectionEnd
            val start = mTokenizer?.findTokenStart(text, end)?:0

            performFiltering(text, start, end, keyCode)
        } else {
            dismissDropDown()

            val f = filter
            f?.filter(null)
        }
    }

    /**
     * Instead of filtering whenever the total length of the text
     * exceeds the threshhold, this subclass filters only when the
     * length of the range from
     * [Tokenizer.findTokenStart] to [.getSelectionEnd]
     * meets or exceeds [.getThreshold].
     */
    override fun enoughToFilter(): Boolean {
        val text = text

        val end = selectionEnd
        if (end < 0 || mTokenizer == null) {
            return false
        }

        val start = mTokenizer?.findTokenStart(text, end)?:0

        return end - start >= threshold
    }

    /**
     * Instead of validating the entire text, this subclass method validates
     * each token of the text individually.  Empty tokens are removed.
     */
    override fun performValidation() {
        val v = validator

        if (v == null || mTokenizer == null) {
            return
        }

        val e = text
        var i = text.length
        while (i > 0) {
            val start = mTokenizer?.findTokenStart(e, i) ?: 0
            val end = mTokenizer?.findTokenEnd(e, start) ?: 0

            val sub = e.subSequence(start, end)
            if (TextUtils.isEmpty(sub)) {
                e.replace(start, i, "")
            } else if (!v.isValid(sub)) {
                e.replace(
                    start, i,
                    mTokenizer?.terminateToken(v.fixText(sub))
                )
            }

            i = start
        }
    }

    /**
     *
     * Starts filtering the content of the drop down list. The filtering
     * pattern is the specified range of text from the edit box. Subclasses may
     * override this method to filter with a different pattern, for
     * instance a smaller substring of `text`.
     */
    protected fun performFiltering(
        text: CharSequence, start: Int, end: Int,
        keyCode: Int
    ) {
        filter.filter(text.subSequence(start, end), this)
    }

    /**
     *
     * Performs the text completion by replacing the range from
     * [Tokenizer.findTokenStart] to [.getSelectionEnd] by the
     * the result of passing `text` through
     * [Tokenizer.terminateToken].
     * In addition, the replaced region will be marked as an AutoText
     * substition so that if the user immediately presses DEL, the
     * completion will be undone.
     * Subclasses may override this method to do some different
     * insertion of the content into the edit box.
     *
     * @param text the selected suggestion in the drop down list
     */
    override fun replaceText(text: CharSequence) {

        clearComposingText()

        val end = selectionEnd
        val start = getTokenStart(end)

        val editable = getText()

        val original = TextUtils.substring(editable, start, end)

        QwertyKeyListener.markAsReplaced(editable, start, end, original)
        editable.replace(start, end, mTokenizer?.terminateToken(text))
    }


    /**
     * removeTokenAfterSelection
     * @param isRemove to remove token after text is selected
     */
    fun removeTokenAfterSelection(isRemove: Boolean?) {
        mIsRemoveToken = isRemove?:false
    }

    private fun getTokenStart(end: Int): Int {
        return if (mIsRemoveToken) {
            mTokenizer?.findTokenStart(text, end)?:0 - 1
        } else
            mTokenizer?.findTokenStart(text, end)?:0
    }


    override fun getAccessibilityClassName(): CharSequence {
        return SulekhaAutoCompleteTextView::class.java.name
    }

    interface Tokenizer {
        /**
         * Returns the start of the token that ends at offset
         * `cursor` within `text`.
         */
        fun findTokenStart(text: CharSequence, cursor: Int): Int

        /**
         * Returns the end of the token (minus trailing punctuation)
         * that begins at offset `cursor` within `text`.
         */
        fun findTokenEnd(text: CharSequence, cursor: Int): Int

        /**
         * Returns `text`, modified, if necessary, to ensure that
         * it ends with a token terminator (for example a space or /).
         */
        fun terminateToken(text: CharSequence): CharSequence
    }

    /**
     * This simple Tokenizer can be used for lists where the items are
     * separated by a token and one or more spaces.
     * Set Token via constructor
     */

    class SulekhaTokenizer (private val mToken: Char) : Tokenizer {

        override fun terminateToken(text: CharSequence): CharSequence {
            var i = text.length

            while (i > 0 && text[i - 1] == ' ') {
                i--
            }

            if (i > 0 && text[i - 1] == ' ') {
                return text
            } else {
                if (text is Spanned) {
                    text[0]
                    val sp = SpannableString("$text ")

                    TextUtils.copySpansFrom(text, 0, text.length, Any::class.java, sp, 0)
                    return sp
                } else {
                    return "$text "
                }
            }
        }

        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor

            while (i > 0 && text[i - 1] != mToken) {
                i--
            }

            //Check if token really started with /, else we don't have a valid token
            return if (i < 1 || text[i - 1] != mToken) {
                cursor
            } else i
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            val len = text.length

            while (i < len) {
                if (text[i] == ' ') {
                    return i
                } else {
                    i++
                }
            }

            return len
        }
    }
}

