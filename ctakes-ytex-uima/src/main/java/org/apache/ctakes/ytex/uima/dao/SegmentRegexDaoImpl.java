package org.apache.ctakes.ytex.uima.dao;


import java.util.List;

import org.apache.ctakes.ytex.uima.model.SegmentRegex;
import org.hibernate.SessionFactory;

public class SegmentRegexDaoImpl implements SegmentRegexDao {
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	private SessionFactory sessionFactory;
	/* (non-Javadoc)
	 * @see gov.va.vacs.esld.dao.SegmentRegex#getSegmentRegexs()
	 */
	@SuppressWarnings("unchecked")
	public List<SegmentRegex> getSegmentRegexs() {
		return (List<SegmentRegex>)sessionFactory.getCurrentSession().createQuery("from SegmentRegex").list();
	}
}
