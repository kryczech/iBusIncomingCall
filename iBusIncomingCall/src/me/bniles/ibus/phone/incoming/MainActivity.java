/* This file is part of iBusIncomingCall.

    iBusIncomingCall is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    iBusIncomingCall is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with iBusIncomingCall.  If not, see <http://www.gnu.org/licenses/>.
    
*/

package me.bniles.ibus.phone.incoming;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;

public class MainActivity extends Activity {
	
	private TextView textViewContactName;
	private TextView textViewContactNumber;
	private ImageView imageViewContactPicture;
	private Button buttonIgnore;
	private Button buttonAnswer;
	
	private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        
        Log.i(TAG, "Getting extras...");
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
        	Log.i(TAG, "No extras found!");
        	return;
        }
        //String phoneNumber = extras.getString("PhoneNumber");
        String phoneNumber = extractPhoneNumberFromMessage(extras.getByteArray("MessageData"));

        buttonIgnore = (Button) findViewById(R.id.buttonIgnore);
        buttonAnswer = (Button) findViewById(R.id.buttonAnswer);
        textViewContactName = (TextView) findViewById(R.id.textView1);
        textViewContactNumber = (TextView) findViewById(R.id.TextView01);
        imageViewContactPicture = (ImageView) findViewById(R.id.imageView1);
        
        textViewContactName.setText("No Name");
        
        textViewContactName.setText(getContactDisplayNameByNumber(phoneNumber));
		imageViewContactPicture.setImageBitmap(loadContactPhoto(phoneNumber));
		textViewContactNumber.setText(formatPhoneNumber(phoneNumber));
		
		OnTouchListener touchTripleAction = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN ) {
					broadcastOutput(Integer.parseInt(v.getTag().toString()));
					return false;
				} else if (event.getAction() == MotionEvent.ACTION_UP ) {
					broadcastOutput(Integer.parseInt(v.getTag().toString()) + 2);
					finish();
					return false;
				} 

				return false;

			}
		};

		OnLongClickListener longTripleAction = new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				broadcastOutput(Integer.parseInt(v.getTag().toString()) + 1);
				return true;
			}
		};
		
		buttonAnswer.setTag("49");
		buttonAnswer.setOnTouchListener(touchTripleAction);
        buttonAnswer.setOnLongClickListener(longTripleAction);
        
        buttonIgnore.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
        		finish();
        	}
        });
        
        // timer to automatically stop activity after 30 seconds
        TimerTask task = new TimerTask() {
        	@Override
        	public void run() {
        		finishscreen();
        	}
        };
        Timer t = new Timer();
        t.schedule(task, 30000);
    }

    private void finishscreen() {
    	this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu; this adds items to the action bar if it is present.
    	getMenuInflater().inflate(R.menu.main, menu);
    	return true;
    }

    public String getContactDisplayNameByNumber(String number) {
    	Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    	String name = "Unknown Caller";

    	ContentResolver contentResolver = getContentResolver();
    	Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

    	try {
    		if (contactLookup != null && contactLookup.getCount() > 0) {
    			contactLookup.moveToNext();
    			name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
    			//String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
    		}
    	} finally {
    		if (contactLookup != null) {
    			contactLookup.close();
    		}
    	}

    	return name;
    }
    
    public Bitmap loadContactPhoto(String number) { 
    	ContentResolver contentResolver = getContentResolver();
    	
    	 
    	Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    	Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

    	try {
    		if (contactLookup != null && contactLookup.getCount() > 0) {
    			contactLookup.moveToNext();
    			// name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
    			long contactId = Long.parseLong(contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID)));
    			Uri imageuri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
    			InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, imageuri);
    			if (input == null) {  
    				return null;    
    			}
    			return BitmapFactory.decodeStream(input);
    		}
    	} finally {
    		if (contactLookup != null) {
    			contactLookup.close();
    		}
    	}
    	return null;

    } 
    
    private String formatPhoneNumber(String number) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("(");
    	sb.append(number.substring(0, 3));
    	sb.append(") ");
    	sb.append(number.substring(3, 6));
    	sb.append("-");
    	sb.append(number.substring(6, 10));
    	return sb.toString();
    }
    
    private String extractPhoneNumberFromMessage(byte[] messageData) {
    	byte[] tempbytes = new byte[messageData.length - 7];
    	String tempstring;
    	for (int i = 0; i < tempbytes.length; i++) {
			tempbytes[i] = messageData[i + 6];
		}
		try {
			tempstring = new String(tempbytes, "UTF-8");
		} catch (Exception e) {
			tempstring = "0000000000";
			Log.e(TAG, "error encoding intent extra");
		}
		return tempstring;
    }
    
    private void broadcastOutput(int messageType) {
    	Intent intent = new Intent();
    	intent.setAction("me.bniles.ibus.addOutputMessage");
		intent.putExtra("iBusMessageType", messageType);
		sendOrderedBroadcast(intent, null);
    }

}
