import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { PatientProfileProvider } from './context/PatientProfileContext';
import { PrivateRoute } from './components/PrivateRoute';
import { AppShell } from './components/layout/AppShell';
import { LoginPage } from './pages/LoginPage';
import { RecipeListPage } from './pages/RecipeListPage';
import { RecipeDetailPage } from './pages/RecipeDetailPage';
import { RecipeFormPage } from './pages/RecipeFormPage';
import { ComponentsPage } from './pages/ComponentsPage';
import { ConditionsPage } from './pages/ConditionsPage';
import { PatientProfilePage } from './pages/PatientProfilePage';
import { ActiveOrdersPage } from './pages/ActiveOrdersPage';
import { OrderHistoryPage } from './pages/OrderHistoryPage';
import { KitchenPage } from './pages/KitchenPage';
import { DoctorPage } from './pages/DoctorPage';

export default function App() {
  return (
    <AuthProvider>
      <PatientProfileProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* All routes inside PrivateRoute require authentication */}
          <Route element={<PrivateRoute />}>
            <Route element={<AppShell />}>
              <Route path="/recipes" element={<RecipeListPage />} />
              <Route path="/recipes/new" element={<RecipeFormPage />} />
              <Route path="/recipes/:id" element={<RecipeDetailPage />} />
              <Route path="/recipes/:id/edit" element={<RecipeFormPage />} />
              <Route path="/components" element={<ComponentsPage />} />
              <Route path="/conditions" element={<ConditionsPage />} />
              <Route path="/profile" element={<PatientProfilePage />} />
              <Route path="/orders/active" element={<ActiveOrdersPage />} />
              <Route path="/orders/history" element={<OrderHistoryPage />} />
              <Route path="/kitchen" element={<KitchenPage />} />
              <Route path="/doctor" element={<DoctorPage />} />
              <Route path="*" element={<Navigate to="/recipes" replace />} />
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/recipes" replace />} />
        </Routes>
      </PatientProfileProvider>
    </AuthProvider>
  );
}
