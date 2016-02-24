package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";
    private final int LOADER_ID = 1;
    private final String EAN_CONTENT = "eanContent";
    private EditText ean;
    private TextView bookTitleView;
    private TextView subTitleView;
    private TextView authorsView;
    private TextView categoriesView;
    private View saveBtn;
    private View deleteBtn;
    private ImageView coverView;
    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";
    private String mToastMsg;


    public AddBook() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ean != null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);
        bookTitleView = (TextView) rootView.findViewById(R.id.bookTitle);
        subTitleView = (TextView) rootView.findViewById(R.id.bookSubTitle);
        authorsView = (TextView) rootView.findViewById(R.id.authors);
        categoriesView = (TextView) rootView.findViewById(R.id.categories);
        Button scanBtn = (Button) rootView.findViewById(R.id.scan_button);
        saveBtn = rootView.findViewById(R.id.save_button);
        deleteBtn = rootView.findViewById(R.id.delete_button);
        coverView = (ImageView) rootView.findViewById(R.id.bookCover);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();

                SharedPreferences sp = getActivity().getSharedPreferences(getActivity()
                        .getResources().getString(R.string.error_shared_pref_key), Context.MODE_PRIVATE);
                String status = sp.getString(getActivity().getResources()
                                .getString(R.string.internet_shared_pref_key),
                        getActivity().getResources().getString(R.string.internet_shared_pref_default));
                if (status.equals(getActivity().getResources().getString(R.string.internet_shared_pref_not_ok))) {
                    Toast.makeText(getActivity(), getActivity().getResources().
                            getString(R.string.no_internet_msg), Toast.LENGTH_LONG).show();
                }
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if (savedInstanceState != null) {
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    /**
     * Using journeyapps/zxing-android-embedded Library
     */
    public void scanBarcode() {
        IntentIntegrator.forSupportFragment(this).initiateScan();
    }

    private void displayToast() {
        if (getActivity() != null && mToastMsg != null) {
            Toast.makeText(getActivity(), mToastMsg, Toast.LENGTH_LONG).show();
            mToastMsg = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                mToastMsg = getActivity().getResources().getString(R.string.user_cancelled_msg);
            } else {
                mToastMsg = getActivity().getResources().
                        getString(R.string.show_code_msg) + result.getContents();
                ean.setText(result.getContents());
            }
            displayToast();
        }
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (ean.getText().length() == 0) {
            return null;
        }
        String eanStr = ean.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        imgUrl += getActivity().getResources().getString(R.string.img_url_affix);
        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));

        // Extra error cases handling:
        // need to examine if the data fetched from the database is empty or not
        if (bookTitle != null && bookTitle.length() != 0)
            bookTitleView.setText(bookTitle);
        if (bookSubTitle != null && bookSubTitle.length() != 0)
            subTitleView.setText(bookSubTitle);
        // For example here:
        // If the authors info is empty, then call split(",") on it will cause NullPointerException
        if (authors != null && authors.length() != 0) {
            String[] authorsArr = authors.split(",");
            authorsView.setLines(authorsArr.length);
            authorsView.setText(authors.replace(",", "\n"));
        }

        // Use picasso library to handling image loading and caching
        coverView.setVisibility(View.VISIBLE);
        Picasso.with(getContext())
                .load(imgUrl)
                .placeholder(R.drawable.coverless)
                .error(R.drawable.coverless)
                .into(coverView);

        if (categories != null && categories.length() != 0)
            categoriesView.setText(categories);

        saveBtn.setVisibility(View.VISIBLE);
        deleteBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields() {
        bookTitleView.setText("");
        subTitleView.setText("");
        authorsView.setText("");
        categoriesView.setText("");
        coverView.setVisibility(View.INVISIBLE);
        saveBtn.setVisibility(View.INVISIBLE);
        deleteBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
