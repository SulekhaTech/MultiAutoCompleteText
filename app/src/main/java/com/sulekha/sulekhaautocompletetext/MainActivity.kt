package com.sulekha.sulekhaautocompletetext

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Sanaullah
 * @createdAt 21/03/2019
 */
class MainActivity : AppCompatActivity() {

    var array = arrayOf("Thanks", "Welcome", "Good","Great","Kindness","Politeness","Cool","Sulekha")

    var adapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /**
         * Set Tokenizer with '/' as token
         */
        etAutoComplete.setTokenizer(SulekhaAutoCompleteTextView.SulekhaTokenizer('/'))

        /**
         * Remove Token '/' after selection
         */
        etAutoComplete.removeTokenAfterSelection(true)
        etAutoComplete.threshold = 1

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, array)
        etAutoComplete.setAdapter(adapter)
    }
}
