package com.trg.dao.hibernate;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.trg.dao.MetaDataUtil;
import com.trg.dao.search.Search;
import com.trg.dao.search.SearchResult;

/**
 * Base class for DAOs that uses Hibernate SessionFactory and HQL for searches.
 * The SessionFactory property is annotated for automatic resource injection.
 * 
 * @author dwolverton
 * 
 */
@SuppressWarnings("unchecked")
public class BaseDAOImpl {

	private HibernateSearchProcessor searchProcessor;

	private SessionFactory sessionFactory;

	private MetaDataUtil metaDataUtil;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		searchProcessor = HibernateSearchProcessor
				.getInstanceForSessionFactory(sessionFactory);
		metaDataUtil = HibernateMetaDataUtil
				.getInstanceForSessionFactory(sessionFactory);
	}

	protected SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	protected MetaDataUtil getMetaDataUtil() {
		return metaDataUtil;
	}

	protected HibernateSearchProcessor getSearchProcessor() {
		return searchProcessor;
	}

	/**
	 * <p>
	 * Persist the given transient instance and add it to the datastore, first
	 * assigning a generated identifier. (Or using the current value of the
	 * identifier property if the assigned generator is used.) This operation
	 * cascades to associated instances if the association is mapped with
	 * cascade="save-update".
	 * 
	 * <p>
	 * Also it seems this method only works with generated ids. If an entity is
	 * passed in that already has an id assigned, an exception is thrown.
	 * 
	 * <p>
	 * This is different from <code>persist()</code> in that it does guarantee
	 * that the object will be assigned an identifier immediately. With
	 * <code>save()</code> a call is made to the datastore immediately if the id
	 * is generated by the datastore so that the id can be determined. With
	 * <code>persist</code> this call may not occur until flush time.
	 * 
	 * @return The id of the newly saved entity.
	 */
	protected Serializable _save(Object object) {
		return getSession().save(object);
	}

	/**
	 * <p>
	 * Make a transient instance persistent and add it to the datastore. This
	 * operation cascades to associated instances if the association is mapped
	 * with cascade="persist". Throws an error if the entity already exists.
	 * 
	 * <p>
	 * This is different from <code>save()</code> in that it does not guarantee
	 * that the object will be assigned an identifier immediately. With
	 * <code>save()</code> a call is made to the datastore immediately if the id
	 * is generated by the datastore so that the id can be determined. With
	 * <code>persist</code> this call may not occur until flush time.
	 */
	protected void _persist(Object... entities) {
		for (Object entity : entities) {
			getSession().persist(entity);
		}
	}

	/**
	 * Remove the entity of the specified class with the specified id from the
	 * datastore.
	 * 
	 * @return <code>true</code> if the object is found in the datastore and
	 *         deleted, <code>false</code> if the item is not found.
	 */
	protected boolean _deleteById(Class<?> klass, Serializable id) {
		if (id != null) {
			Object entity = getSession().get(klass, id);
			if (entity != null) {
				getSession().delete(entity);
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the specified enity from the datastore.
	 * 
	 * @return <code>true</code> if the object is found in the datastore and
	 *         removed, <code>false</code> if the item is not found.
	 */
	protected boolean _deleteEntity(Object entity) {
		if (entity != null) {
			Serializable id = getMetaDataUtil().getId(entity);
			if (id != null) {
				entity = getSession().get(entity.getClass(), id);
				if (entity != null) {
					getSession().delete(entity);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return the persistent instance of the given entity class with the given
	 * identifier, or null if there is no such persistent instance.
	 * <code>get()</code> always hits the database immediately.
	 */
	protected <T> T _get(Class<T> klass, Serializable id) {
		return (T) getSession().get(klass, id);
	}

	/**
	 * <p>
	 * Return the persistent instance of the given entity class with the given
	 * identifier, assuming that the instance exists. Throw an unrecoverable
	 * exception if there is no matching database row.
	 * 
	 * <p>
	 * If the class is mapped with a proxy, <code>load()</code> just returns an
	 * uninitialized proxy and does not actually hit the database until you
	 * invoke a method of the proxy. This behaviour is very useful if you wish
	 * to create an association to an object without actually loading it from
	 * the database. It also allows multiple instances to be loaded as a batch
	 * if batch-size is defined for the class mapping.
	 */
	protected <T> T _load(Class<T> klass, Serializable id) {
		return (T) getSession().load(klass, id);
	}

	/**
	 * Read the persistent state associated with the given identifier into the
	 * given transient instance. Throw an unrecoverable exception if there is no
	 * matching database row.
	 */
	protected void _load(Object transientEntity, Serializable id) {
		getSession().load(transientEntity, id);
	}

	/**
	 * Get a list of all the objects of the specified class.
	 */
	protected <T> List<T> _all(Class<T> klass) {
		return getSession().createCriteria(klass).setResultTransformer(
				Criteria.DISTINCT_ROOT_ENTITY).list();
	}

	/**
	 * <p>
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier, an
	 * exception is thrown. This operation cascades to associated instances if
	 * the association is mapped with cascade="save-update".
	 * 
	 * <p>
	 * The difference between <code>update()</code> and <code>merge()</code> is
	 * significant: <code>update()</code> will make the given object persistent
	 * and throw and error if another object with the same ID is already
	 * persistent in the Session. <code>merge()</code> doesn't care if another
	 * object is already persistent, but it also doesn't make the given object
	 * persistent; it just copies over the values to the datastore.
	 */
	protected void _update(Object... transientEntities) {
		for (Object entity : transientEntities) {
			getSession().update(entity);
		}
	}

	/**
	 * <p>
	 * Copy the state of the given object onto the persistent object with the
	 * same identifier. If there is no persistent instance currently associated
	 * with the session, it will be loaded. Return the persistent instance. If
	 * the given instance is unsaved, save a copy of and return it as a newly
	 * persistent instance. The given instance does not become associated with
	 * the session. This operation cascades to associated instances if the
	 * association is mapped with cascade="merge".
	 * 
	 * <p>
	 * The difference between <code>update()</code> and <code>merge()</code> is
	 * significant: <code>update()</code> will make the given object persistent
	 * and throw and error if another object with the same ID is already
	 * persistent in the Session. <code>merge()</code> doesn't care if another
	 * object is already persistent, but it also doesn't make the given object
	 * persistent; it just copies over the values to the datastore.
	 */
	protected <T> T _merge(T entity) {
		return (T) getSession().merge(entity);
	}

	/**
	 * Search for objects based on the search parameters in the specified
	 * <code>Search</code> object.
	 * 
	 * @see Search
	 */
	protected List _search(Search search) {
		if (search == null)
			throw new NullPointerException("search is null");
		return getSearchProcessor().search(getSession(), search);
	}

	/**
	 * Same as <code>_search(Search)</code> except that it forces the search to use the specified
	 * search class when no other search class is assigned to the search. If the
	 * search does have a different search class assigned, an exception is
	 * thrown.
	 */
	protected List _search(Search search, Class<?> forceClass) {
		if (forceClass == null)
			return _search(search);
		if (search == null)
			throw new NullPointerException("search is null");

		boolean classNull = getSearchProcessor().forceSearchClass(search,
				forceClass);
		List result = _search(search);
		if (classNull)
			search.setSearchClass(null);
		return result;
	}

	/**
	 * Returns the total number of results that would be returned using the
	 * given <code>Search</code> if there were no paging or maxResult limits.
	 * 
	 * @see Search
	 */
	protected int _count(Search search) {
		if (search == null)
			throw new NullPointerException("search is null");
		return getSearchProcessor().count(getSession(), search);
	}

	/**
	 * Same as <code>_count(Search)</code> except that it forces the search to use the specified
	 * search class when no other search class is assigned to the search. If the
	 * search does have a different search class assigned, an exception is
	 * thrown.
	 */
	protected int _count(Search search, Class<?> forceClass) {
		if (forceClass == null)
			return _count(search);
		if (search == null)
			throw new NullPointerException("search is null");

		boolean classNull = getSearchProcessor().forceSearchClass(search,
				forceClass);
		int result = _count(search);
		if (classNull)
			search.setSearchClass(null);
		return result;
	}

	/**
	 * Returns the number of instances of this class in the datastore.
	 * 
	 * @param klass
	 * @return
	 */
	protected int _count(Class<?> klass) {
		List counts = getSession().createQuery(
				"select count(*) from " + klass.getName()).list();
		int sum = 0;
		for (Object count : counts) {
			sum += ((Long) count).intValue();
		}
		return sum;
	}

	/**
	 * Returns a <code>SearchResult</code> object that includes the list of
	 * results like <code>search()</code> and the total length like
	 * <code>searchLength</code>.
	 * 
	 * @see Search
	 */
	protected SearchResult _searchAndCount(Search search) {
		if (search == null)
			throw new NullPointerException("search is null");
		return getSearchProcessor().searchAndCount(getSession(), search);
	}

	/**
	 * Same as <code>_searchAndCount(Search)</code> except that it forces the search to use the specified
	 * search class when no other search class is assigned to the search. If the
	 * search does have a different search class assigned, an exception is
	 * thrown.
	 */
	protected SearchResult _searchAndCount(Search search, Class<?> forceClass) {
		if (forceClass == null)
			return _searchAndCount(search);
		if (search == null)
			throw new NullPointerException("search is null");

		boolean classNull = getSearchProcessor().forceSearchClass(search,
				forceClass);
		SearchResult result = _searchAndCount(search);
		if (classNull)
			search.setSearchClass(null);
		return result;
	}

	/**
	 * Search for a single result using the given parameters.
	 */
	protected Object _searchUnique(Search search)
			throws NonUniqueResultException {
		if (search == null)
			throw new NullPointerException("search is null");
		return getSearchProcessor().searchUnique(getSession(), search);
	}

	/**
	 * Same as <code>_searchUnique(Search)</code> except that it forces the search to use the specified
	 * search class when no other search class is assigned to the search. If the
	 * search does have a different search class assigned, an exception is
	 * thrown.
	 */
	protected Object _searchUnique(Search search, Class<?> forceClass) {
		if (forceClass == null)
			return _searchUnique(search);
		if (search == null)
			throw new NullPointerException("search is null");

		boolean classNull = getSearchProcessor().forceSearchClass(search,
				forceClass);
		Object result = _searchUnique(search);
		if (classNull)
			search.setSearchClass(null);
		return result;
	}

	/**
	 * Returns true if the object is connected to the current hibernate session.
	 */
	protected boolean _isAttached(Object o) {
		return getSession().contains(o);
	}

	/**
	 * Flushes changes in the hibernate cache to the datastore.
	 */
	protected void _flush() {
		getSession().flush();
	}

	/**
	 * Refresh the content of the given entity from the current datastore state.
	 */
	protected void _refresh(Object o) {
		getSession().refresh(o);
	}
}