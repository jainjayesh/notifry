/**
 * Notifry for Android.
 * 
 * Copyright 2011 Daniel Foote
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.notifry.android.database;

import java.util.ArrayList;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public abstract class ORM<T>
{
	protected static final String TAG = "Notifry";
	
	/**
	 * The ID of this object.
	 */
	protected Long id;
	
	/**
	 * Save this object to the database.
	 * @param context
	 */
	public void save( Context context )
	{
		if( this.getId() == null )
		{
			// Insert.
			ContentValues values = this.flatten();
			Uri uri = context.getContentResolver().insert(this.getContentUri(), values);
			this.setId(Long.parseLong(uri.getPathSegments().get(1)));
		}
		else
		{
			// Update.
			ContentValues values = this.flatten();
			context.getContentResolver().update(this.getItemUri(), values, NotifryDatabaseAdapter.KEY_ID + "=" + this.getId(), null);
		}
	}
	
	/**
	 * Get the URI of this item.
	 * @return
	 */
	public Uri getItemUri()
	{
		return ContentUris.withAppendedId(this.getContentUri(), this.getId());
	}
	
	/**
	 * Delete this object.
	 * @param context
	 */
	public void delete( Context context )
	{
		context.getContentResolver().delete(this.getItemUri(), NotifryDatabaseAdapter.KEY_ID + "=" + this.getId(), null);
	}
	
	/**
	 * Factory method - delete the object when only the ID is known.
	 * @param context
	 * @param id
	 */
	public void deleteById( Context context, Long id )
	{
		context.getContentResolver().delete(this.getItemUri(), NotifryDatabaseAdapter.KEY_ID + "=" + id, null);
	}
	
	/**
	 * Generic delete - delete by arbitrary parameters.
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 */
	protected void genericDelete( Context context, String selection, String[] selectionArgs )
	{
		context.getContentResolver().delete(this.getContentUri(), selection, selectionArgs);
	}
	
	/**
	 * Get a single instance of this class by ID.
	 * @param context
	 * @param id
	 * @return
	 */
	public T get( Context context, Long id )
	{
		return this.getOne(context, NotifryDatabaseAdapter.KEY_ID + "=" + id, null);
	}
	
	/**
	 * Get the ID of this object - NULL if not yet saved.
	 * @return
	 */
	public Long getId()
	{
		return this.id;
	}

	/**
	 * Set the ID of this object.
	 * @param id
	 */
	protected void setId( Long id )
	{
		this.id = id;
	}
	
	/**
	 * List entries from the database, inflating them as required.
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @return
	 */
	protected ArrayList<T> genericList( Context context, String selection, String[] selectionArgs, String sortOrder )
	{
		Cursor cursor = context.getContentResolver().query(this.getContentUri(), this.getProjection(), selection, selectionArgs, sortOrder);
		ArrayList<T> result = new ArrayList<T>();
		if( cursor.moveToFirst() )
		{
			do
			{
				result.add(this.inflate(context, cursor));
			}
			while( cursor.moveToNext() );
		}
		cursor.close();
		return result;
	}
	
	/**
	 * Count entries from the database, without inflating them.
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 * @return
	 */
	protected int genericCount( Context context, String selection, String[] selectionArgs )
	{
		Cursor cursor = context.getContentResolver().query(this.getContentUri(), this.getProjection(), selection, selectionArgs, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}	
	
	/**
	 * Get a single entry from the database matching the query, or NULL if not found.
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 * @return
	 */
	protected T getOne( Context context, String selection, String[] selectionArgs )
	{
		ArrayList<T> list = this.genericList(context, selection, selectionArgs, null);
		
		if( list.size() == 0 )
		{
			return null;
		}
		else
		{
			return list.get(0);
		}
	}

	/**
	 * Get the content URI for this ORM type.
	 * @return
	 */
	public abstract Uri getContentUri();
	
	/**
	 * Flatten the objects data into a set of content values.
	 * @return
	 */
	protected abstract ContentValues flatten();
	/**
	 * Inflate this object from the given cursor.
	 * @param cursor
	 * @return
	 */
	protected abstract T inflate( Context context, Cursor cursor );
	/**
	 * Get the projection required when querying this object.
	 * @return
	 */
	protected abstract String[] getProjection();
}