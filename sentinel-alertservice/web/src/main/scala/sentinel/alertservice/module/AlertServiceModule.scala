package sentinel.alertservice.module

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import sentinel.alertservice.database.{DataStore, DummyDataStore}

object AlertServiceModule extends TwitterModule{

  @Singleton
  @Provides
  def dataStore: DataStore = new DummyDataStore

}
